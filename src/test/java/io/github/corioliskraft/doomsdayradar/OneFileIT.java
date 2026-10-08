package io.github.corioliskraft.doomsdayradar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.util.Map;

@Testcontainers
class OneFileIT {

    @Container static final RadarContainers containers = new RadarContainers();

    @Test
    void endpointReturnsTheAircraftCountOfEachSquareInOneFile(@TempDir Path dir) throws Exception {
        var file = HeatmapFixture.writeBerlinAndParis(dir.resolve("heatmap.bin.ttf"));
        var settings = containers.settings();

        try (var service = RadarService.start(settings, 0)) {
            assertThat(SquaresClient.importAndAwait(file, settings, service.port(), 2))
                    .containsExactlyInAnyOrder(
                            Map.of("latitude", 52, "longitude", 13, "aircraft", 2),
                            Map.of("latitude", 48, "longitude", 2, "aircraft", 1));
        }
    }
}
