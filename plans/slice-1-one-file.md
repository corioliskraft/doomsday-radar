# Plan: One file

**Branch**: main
**Status**: Active

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
- **Libraries** (accepted 2026-10-06): `kafka-clients` 4.3.1, ClickHouse `client-v2` 0.10.0, Testcontainers 2.0.5, Confluent Schema Registry 8.3.2 with `kafka-avro-serializer` 8.3.2 and Avro 1.12.2, the JDK HTTP server, MapLibre GL JS 6.12.0 with OpenFreeMap tiles. Leaflet lost because its last stable release is from 2023.

### Slice 2: count each aircraft once

The count for a square is the number of unique aircraft in the slot. Header, marker and callsign records do not count. The ICAO hex codes are stored.

- **RED**: unit tests with fixtures that have repeated positions and each record type.
- **Check**: the file `2026/10/05/heatmap/26` gives 17,721 aircraft in 4,394 squares.

### Slice 3: download the file

The job takes a date and a slot number, downloads the file, processes it, and deletes it. If the file is missing, the job stops with an error and sends nothing.

### Slice 4: data as of

The page shows "data as of <slot time>".
