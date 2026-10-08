package io.github.corioliskraft.doomsdayradar;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

final class HeatmapFile {

    private static final int RECORD_BYTES = 16;
    private static final double MICRODEGREES_PER_DEGREE = 1_000_000.0;

    static Collection<SquareCount> countPerSquare(Path file) throws IOException {
        ByteBuffer records;
        try (var in = new GZIPInputStream(Files.newInputStream(file))) {
            records = ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
        }
        var counts = new TreeMap<String, SquareCount>();
        while (records.remaining() >= RECORD_BYTES) {
            records.getInt();
            var latitude = (int) Math.floor(records.getInt() / MICRODEGREES_PER_DEGREE);
            var longitude = (int) Math.floor(records.getInt() / MICRODEGREES_PER_DEGREE);
            records.getInt();
            var count =
                    counts.computeIfAbsent(
                            latitude + "," + longitude,
                            _ -> new SquareCount(latitude, longitude, 0));
            count.setAircraft(count.getAircraft() + 1);
        }
        return counts.values();
    }

    private HeatmapFile() {}
}
