# Walking skeleton

## Idea

1. A backfill job downloads the files of the last 29 days (approximately 32 GB). For each file, the job counts the unique aircraft in each 1° square for each 30-minute slot. The job keeps the counts and the ICAO hex codes for each square and slot, and deletes the file.
2. Every 30 minutes, a poll job does the same steps for the newest file.
3. The baseline for each square and slot is the mean of the values on day −7, −14, −21, and −28.
4. A web page shows a world map with squares of 1° by 1°. The map is white for land and water, with thin lines for the coastlines and the country borders. The colour of each square compares the current number of aircraft with the baseline:
   - green: near normal
   - red: much fewer aircraft
   - blue: many more aircraft
   - white: no data

   The limits for each colour are not selected yet.

## First steps

1. **One file.** A Java job reads one file and sends the counts for each square through Kafka to ClickHouse. The ICAO hex codes follow in slice 2. Result: the world map shows each square in a shade of grey: white for 0 aircraft, darker for more aircraft.
2. **Four weeks of history, baseline, and colours.** A backfill job does step 1 for each file of the last 29 days. The processing job calculates the baseline and the status of each square. Result: each square has the colour of its status.
3. **Live data.** A poll job does step 1 for the newest file every 30 minutes. Result: the map shows the latest slot.

## Rules

- If data is missing, the square is white. Missing data will be ignored when calculating the baseline.
- A square without aircraft in the baseline has no data and is white. A square with aircraft in the baseline and 0 aircraft in the current slot is red.
- The page shows "data as of <...>".

## Technical design

Data flow:

1. A Java job downloads a file, counts the aircraft for each square, and sends the counts and ICAO hex codes to Kafka.
2. A processing job reads Kafka. For each square, it reads the four baseline slots from the database, calculates the mean, and selects the status: normal, too few, too many, or no data. It writes the count, the ICAO hex codes, the baseline, and the status to the database. In version 1, the processing job is a Java consumer. Later, Spark can do this step.
3. The web page reads the statuses from the database and shows each square on the map in the colour of its status.

| Part | Decision | Reason |
|---|---|---|
| Java | Java 25 | Newest LTS, supports all tools that we need. |
| Kafka | Now, one broker | All later sources send their data through Kafka. |
| Message format | Avro with Confluent Schema Registry, now | Kafka accepts any bytes. The producer serializer checks each message against the registered schema and refuses a message that does not match, before the message is sent. |
| Database | ClickHouse, now | Free and open source (Apache 2.0). It runs locally in Docker, has a Testcontainers module, a Kafka table engine, unique counts, and geographic functions. This is sufficient for our needs. |
| Docker | Now | Kafka and ClickHouse run in containers on the laptop. Testcontainers also needs Docker. |
| Map page | MapLibre GL JS 6.12.0 served by the project, OpenFreeMap vector tiles, now | Leaflet has no stable release since 2023. OpenFreeMap needs no key. The browser loads the tiles from OpenFreeMap at runtime. |
| Spark | Later | A Java consumer is sufficient for one file every 30 minutes, and its tests are fast. A later change to Spark rewrites only the processing job. |
| Hosting, Kubernetes, S3 | Later | Version 1 runs on a laptop in Docker. |

## Data source

- URL: `https://adsb.lol/globe_history/YYYY/MM/DD/heatmap/NN.bin.ttf`. `NN` is `00` to `47`. There is one file for each 30 minutes (UTC).
- The body of the file is in gzip format.
- Content: a snapshot every 10 seconds with positions and ICAO hex codes. The file for 2026-10-05, 13:00 to 13:30 UTC: 23 MB, 17,721 unique aircraft, 4,394 squares with data.
- Example of a delay: the file for 13:00 to 13:30 UTC on 2026-10-05 was available at 13:47 UTC.

## Important

- Keep the ICAO hex codes for each square and slot, not only the counts. A later merge with OpenSky uses these codes to remove duplicates.
- The heatmap path is not a documented API. Files can be missing. For example, the file `2026/03/01/heatmap/00` returned HTTP 404.
