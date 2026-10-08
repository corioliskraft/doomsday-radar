package io.github.corioliskraft.doomsdayradar;

import com.microsoft.playwright.Page;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

final class MapScreenshot {

    record Point(double longitude, double latitude) {}

    private final Page page;
    private final byte[] png;
    private final BufferedImage image;

    static MapScreenshot of(Page page) throws IOException {
        var png = page.screenshot();
        return new MapScreenshot(page, png, ImageIO.read(new ByteArrayInputStream(png)));
    }

    private MapScreenshot(Page page, byte[] png, BufferedImage image) {
        this.page = page;
        this.png = png;
        this.image = image;
    }

    void saveTo(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, png);
    }

    Color colourAt(Point point) {
        var screen =
                (List<?>)
                        page.evaluate(
                                """
                                ([longitude, latitude]) => {
                                    const p = window.map.project([longitude, latitude]);
                                    return [p.x, p.y];
                                }
                                """,
                                List.of(point.longitude(), point.latitude()));
        var x = Math.round(((Number) screen.get(0)).floatValue());
        var y = Math.round(((Number) screen.get(1)).floatValue());
        return new Color(image.getRGB(x, y));
    }
}
