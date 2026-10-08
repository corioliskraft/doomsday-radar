package io.github.corioliskraft.doomsdayradar;

import static io.github.corioliskraft.doomsdayradar.HeatmapFixture.position;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class HeatmapFileTest {

    @Test
    void aPositionCountsInTheSquareWhoseSouthWestCornerIsBelowIt(@TempDir Path dir)
            throws Exception {
        var file =
                HeatmapFixture.write(
                        dir.resolve("heatmap.bin.ttf"),
                        position(0xA00001, 52.0, 13.5),
                        position(0xA00002, 51.999999, 13.5),
                        position(0xA00003, 48.5, -0.5),
                        position(0xA00004, -33.9, 151.2));

        assertThat(HeatmapFile.countPerSquare(file))
                .containsExactlyInAnyOrder(
                        new SquareCount(52, 13, 1),
                        new SquareCount(51, 13, 1),
                        new SquareCount(48, -1, 1),
                        new SquareCount(-34, 151, 1));
    }
}
