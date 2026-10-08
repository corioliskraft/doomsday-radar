package io.github.corioliskraft.doomsdayradar;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

final class HeatmapFixture {

    record Position(int icao, double latitude, double longitude) {}

    static Position position(int icao, double latitude, double longitude) {
        return new Position(icao, latitude, longitude);
    }

    static Path write(Path file, Position... positions) throws IOException {
        var records = ByteBuffer.allocate(16 * positions.length).order(ByteOrder.LITTLE_ENDIAN);
        for (var position : positions) {
            records.putInt(position.icao())
                    .putInt((int) Math.round(position.latitude() * 1_000_000))
                    .putInt((int) Math.round(position.longitude() * 1_000_000))
                    .putInt(0);
        }
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(file))) {
            out.write(records.array());
        }
        return file;
    }

    private HeatmapFixture() {}
}
