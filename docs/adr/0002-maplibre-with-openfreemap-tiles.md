# ADR-0002: MapLibre GL JS, served by the project, with OpenFreeMap tiles

**Status**: Accepted

**Date**: 2026-10-06 (library choice), 2026-10-08 (served by the project)

**Tags**: web, map, maplibre, openfreemap, third-party

## Context

The page shows a world map with a colour square for each 1° square. The base map needs coastlines and country borders (see [slice-1-one-file.md](../../plans/slice-1-one-file.md)).

## Decision

- The map library is MapLibre GL JS 6.12.0.
- `MapServer` serves the library files under `/maplibre/`. They are copied from the npm tarball into `src/main/resources/web/maplibre/`, and the sha512 was checked against npm.
- The tiles are the OpenFreeMap vector tiles. The page builds its own style on them.
- The browser loads the tiles from OpenFreeMap at runtime. OpenFreeMap needs no key.

## Alternatives considered

### Leaflet

**Why rejected**: the last stable release on npm is 1.9.4 of 2023-05-18.

### Library from a CDN

The step 1.5 test requires that the page loads scripts and stylesheets only from the service itself. Its RED case was a request to unpkg.com.

**Why rejected**: the reason is not recorded in the sources.

### Other tile sources

Not recorded in the sources.

## Consequences

### Positive

- The scripts and styles come from the service, not from a CDN.
- No API key to manage.

### Negative

- The map tiles come from a third party at runtime. If OpenFreeMap is down or changes its terms, the base map breaks.
- The test of the base map reads the real OpenFreeMap tiles, so it is not repeatable offline.
- The project holds a copy of the library files. An update of MapLibre is a manual copy.

## Related

- [walking-skeleton.md](../walking-skeleton.md): row "Map page" of the technical design.
