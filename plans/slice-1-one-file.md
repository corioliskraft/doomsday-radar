# Plan: One file

**Branch**: slice-1-one-file
**Status**: implemented

## Goal

The map shows the aircraft count of one heatmap file for each 1° square, in shades of grey.

## Real-data check (2026-10-06)

- `2026/10/05/heatmap/26` and `2026/10/06/heatmap/00` return HTTP 206 for a range request.
- File format, from the decode of `2026/10/05/heatmap/26`: gzip body, records of four signed 32-bit little-endian integers `f1..f4`.
  - Header records (`f2 = f3 = f4 = 0`) at the start.
  - Marker records (`f1 = 0x0e7f7c9d`): time in ms UTC = `f2 * 2^32 + unsigned f3`, one every 10 s.
  - Callsign records: high 16 bits of `f2` = `0x4000`.
  - All other records are positions. ICAO hex = lower 24 bits of `f1`. `f2` and `f3` ranges fit latitude and longitude in millionths of a degree.
- Result for that file: 17,721 unique aircraft in 4,394 squares.

## Slices

All slices are behavior changes and use `tdd`, `testing` and `refactoring`. Mutation gate: `N/A` until a JVM mutation tool is chosen; the alternate evidence is the RED-GREEN record of each test.

### Slice 1: walking skeleton

An operator runs the job on a local file, and the page shows a grey square for each square with aircraft.

- **Path**: job (file path) → decoder → count per square → Kafka → Java consumer → ClickHouse → HTTP endpoint → page.
- **Acceptance criteria** (accepted 2026-10-06):
  1. A gzip fixture has positions of 2 aircraft in the square 52°N 13°E and 1 aircraft in 48°N 2°E. After the job runs on it, the endpoint returns these 2 squares with the counts 2 and 1.
  2. The page shows a MapLibre world map with each square from the endpoint in grey. A higher count gives a darker grey. A browser check confirms this.
  3. The map is white for land and water, with thin lines for the coastlines and the country borders. A square with 0 aircraft is white.
- **RED**: an end-to-end test with Testcontainers (Kafka, Schema Registry, ClickHouse) runs the job on a small fixture with only position records, and reads the counts from the endpoint.
- **Before it starts**: Docker runs; the `developing-kafka-java-client` skill is installed.
- **Libraries** (accepted 2026-10-06): `kafka-clients` 4.3.1, ClickHouse `client-v2` 0.10.0, Testcontainers 2.0.5, Confluent Schema Registry 8.3.2 with `kafka-avro-serializer` 8.3.2 and Avro 1.12.2, the JDK HTTP server, MapLibre GL JS 6.12.0 with OpenFreeMap tiles. Test only: Jackson 3.2.3 reads the endpoint JSON; Playwright for Java 1.63.0 checks the page (added 2026-10-08). Leaflet lost because its last stable release is from 2023.
- **Next increment**: a failure in the consumer thread reaches the caller, with its own failing test. The same increment adds the test for a failed `SquareCountConsumer.start`: it closes the ClickHouse client. That close has no test yet.
**Depends-on:** none

#### Page: criteria 2 and 3 (dev-team build)

Real-data check (2026-10-08): MapLibre 6.12.0 has no `maplibre-gl.js`, only ES modules (`dist/maplibre-gl.mjs`); `dist/maplibre-gl.css` returns 200. The last stable Leaflet release on npm is 1.9.4 of 2023-05-18. The OpenFreeMap TileJSON `https://tiles.openfreemap.org/planet` returns 200, with the layers `water` (`class`) and `boundary` (`admin_level`, `maritime`).

Design:

- `MapServer` serves `GET /` from the class path resource `web/index.html`. Other paths under `/` return 404.
- The page imports MapLibre 6.12.0 from the project: `MapServer` serves `maplibre-gl.mjs`, `maplibre-gl-shared.mjs`, `maplibre-gl-worker.mjs`, `maplibre-gl.css` and `LICENSE.txt` from the class path folder `web/maplibre/`, under `/maplibre/`. The files come from the npm tarball `maplibre-gl-6.12.0.tgz`, sha512 checked on 2026-10-08 against the npm integrity `sha512-DwgganVi2BhNxOpD7ob3lJC0dQQz4HfJfuQQV3XxCY3XdOzFrMqp9ylMXhRzryRsOnIezdZcklWXIIs/Q8JPjA==`; each file equals its copy in the tarball. The page builds its own style on the OpenFreeMap source. The style has only these layers: white background, white `water` fill, a 1 px line on the outline of the `ocean` water class (coastline), a 1 px line for `boundary` with `admin_level` 2 and `maritime` 0 (country borders), and the squares. No labels.
- The first view shows the full world width.
- The square named 52°N 13°E has its south-west corner at latitude 52, longitude 13. It is the polygon from (`longitude`, `latitude`) to (`longitude` + 1, `latitude` + 1).
- Fill colour: white `#ffffff` at 0, `#d9d9d9` at 1, linear to `#404040` at the highest count of the response. If the highest count is 1, each square is `#404040`.
- Browser check: `MapPageIT` opens the page in headless Chromium with Playwright for Java 1.63.0 (test only). It reads the map state through `window.map` and `window.squaresLoaded`, reads the colour of each square from the screenshot pixels (`queryRenderedFeatures` does not return the evaluated `fill-color`), and saves a screenshot to `target/map-page.png`. `MapPageIT` has its own containers, because two test classes on the same ClickHouse would see each other's counts. The container setup moves to one place that both IT classes use.

```gherkin
Feature: Map page

  Scenario: The page shows a MapLibre world map
    When a user opens the page
    Then the page shows a MapLibre map of the full world width

  Scenario: No squares
    Given the endpoint returns an empty list
    When a user opens the page
    Then the page shows no square

  Scenario: Each square with aircraft is grey, darker for more aircraft
    Given the endpoint returns 52°N 13°E with 2 aircraft and 48°N 2°E with 1 aircraft
    When a user opens the page
    Then the squares 52°N 13°E and 48°N 2°E are grey, not white
    And the fill of 52°N 13°E has a lower luminance than the fill of 48°N 2°E
    And no square covers 0°N 0°E

  Scenario: The base map is white with thin coastlines and country borders
    When a user opens the page
    Then land and water are white, with no labels
    And coastlines and country borders are lines of 1 px
```

##### Step 1.2: Serve a MapLibre world map

**Complexity**: standard
**IMPLEMENT**: Playwright 1.63.0 in `pom.xml` (test scope). `MapServer` returns `web/index.html` for `GET /` as `text/html`. The page shows a MapLibre 6.12.0 map of the full world width and sets `window.map`.
**TEST**: `MapPageIT`: the page loads, `window.map.loaded()` is true, and the map bounds cover longitude -180 to 180. Full suite green.
**REFACTOR**: every green; move the container setup to one place for both IT classes.
**Files**: `pom.xml`, `src/main/java/io/github/corioliskraft/doomsdayradar/MapServer.java`, `src/main/resources/web/index.html`, `src/test/java/io/github/corioliskraft/doomsdayradar/MapPageIT.java`, `src/test/java/io/github/corioliskraft/doomsdayradar/OneFileIT.java`
**Commit**: `Serve a MapLibre world map`

##### Step 1.3: Grey squares

**Complexity**: standard
**IMPLEMENT**: The page reads `/api/squares` and draws each square with the fill colour of the design.
**TEST**: `MapPageIT`: before the job runs, no square is rendered. After the job runs on the criterion 1 fixture: both fills are grey and not white, the 52°N 13°E fill has the lower luminance, and no square covers 0°N 0°E. Full suite green.
**REFACTOR**: every green.
**Files**: `src/main/resources/web/index.html`, `src/test/java/io/github/corioliskraft/doomsdayradar/MapPageIT.java`, `src/test/java/io/github/corioliskraft/doomsdayradar/HeatmapFixture.java`, `src/test/java/io/github/corioliskraft/doomsdayradar/SquaresClient.java`
**Commit**: `Show the squares in grey`

##### Step 1.4: White base map

**Complexity**: standard
**IMPLEMENT**: The style layers of the design: white background and water, 1 px coastline and country border lines, no labels.
**TEST**: `MapPageIT`: the style has only the layers of the design, background and water are `#ffffff`, both line layers have width 1, no layer of type `symbol`. The test saves `target/map-page.png`; Claude reads it. Full suite green.
**REFACTOR**: every green.
**Files**: `src/main/resources/web/index.html`, `src/test/java/io/github/corioliskraft/doomsdayradar/MapPageIT.java`
**Commit**: `Show a white base map with coastlines and borders`

##### Step 1.5: Serve MapLibre from the project

**Complexity**: standard
**IMPLEMENT**: Copy the 5 files to `src/main/resources/web/maplibre/`. `MapServer` serves exactly these files under `/maplibre/<name>` with the content types `text/javascript`, `text/css` and `text/plain`; other paths stay 404. The page imports `/maplibre/maplibre-gl.mjs` and links `/maplibre/maplibre-gl.css`.
**TEST**: `MapPageIT`: while the page loads, it requests scripts and stylesheets only from the service itself (RED: requests to unpkg.com). Full suite green.
**REFACTOR**: every green.
**Files**: `src/main/resources/web/maplibre/*`, `src/main/resources/web/index.html`, `src/main/java/io/github/corioliskraft/doomsdayradar/MapServer.java`, `src/test/java/io/github/corioliskraft/doomsdayradar/MapPageIT.java`
**Commit**: `Serve MapLibre from the project`

Farley properties below 6 that stay (2026-10-08):

- `baseMapDrawsCoastlinesAndBordersAndLeavesLandAndOpenOceanWhite`, Repeatable 4 and Fast 5: criterion 3 is about the real coastlines and borders, so the test reads the real OpenFreeMap tiles.
- `endpointReturnsTheAircraftCountOfEachSquareInOneFile`, Fast 4: the end-to-end test runs through Kafka and ClickHouse; the time is the container cost.
- `styleHasBaseMapLayersWhiteFillsAndOnePixelLines`, Maintainable 4: the step 1.4 TEST line requires the exact layer list.

#### Build Progress

- [x] Slice 1: walking skeleton
  - [x] Step 1.1: Count aircraft per square from one local file
  - [x] Step 1.2: Serve a MapLibre world map
  - [x] Step 1.3: Grey squares
  - [x] Step 1.4: White base map
  - [x] Step 1.5: Serve MapLibre from the project

### Slice 2: count each aircraft once

**Depends-on:** 1

The count for a square is the number of unique aircraft in the slot. Header, marker and callsign records do not count. The ICAO hex codes are stored.

- **RED**: unit tests with fixtures that have repeated positions and each record type.
- **Check**: the file `2026/10/05/heatmap/26` gives 17,721 aircraft in 4,394 squares.

### Slice 3: download the file

**Depends-on:** 2

The job takes a date and a slot number, downloads the file, processes it, and deletes it. If the file is missing, the job stops with an error and sends nothing. A file imported twice, or a message delivered twice, does not duplicate the counts. The endpoint returns only the squares of the newest slot, at most 64,800.

### Slice 4: data as of

**Depends-on:** 3

The page shows "data as of <slot time>".
