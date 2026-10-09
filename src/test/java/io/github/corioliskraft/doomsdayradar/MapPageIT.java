package io.github.corioliskraft.doomsdayradar;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Route;

import io.github.corioliskraft.doomsdayradar.MapScreenshot.Point;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Testcontainers
class MapPageIT {

    private static final double WAIT_TIMEOUT_MS = 60_000;
    private static final String MAP_LOADED = "window.map && window.map.loaded()";
    private static final String SQUARES_LOADED = "window.squaresLoaded === true";
    private static final String BASE_MAP_LOADED =
            "window.map.isSourceLoaded('openmaptiles') && window.map.areTilesLoaded()";
    private static final Set<String> SCRIPT_AND_STYLESHEET = Set.of("script", "stylesheet");
    private static final String SQUARES_LAYER = "squares";
    private static final String COASTLINE_LAYER = "coastline";
    private static final String COUNTRY_BORDERS_LAYER = "country-borders";
    private static final List<String> LAYER_IDS =
            List.of("background", "water", COASTLINE_LAYER, COUNTRY_BORDERS_LAYER, SQUARES_LAYER);
    private static final String WHITE = "#ffffff";
    private static final String LINE_GREY = "#808080";
    private static final int LINE_WIDTH = 1;
    private static final Path SCREENSHOT = Path.of("target", "map-page.png");

    private static final Color DARK_GREY = new Color(0x404040);
    private static final Color LIGHT_GREY = new Color(0xD9D9D9);

    private static final Point SQUARE_52N_13E_CENTRE = new Point(13.5, 52.5);
    private static final Point SQUARE_48N_2E_CENTRE = new Point(2.5, 48.5);
    private static final Point SQUARE_0N_0E_CENTRE = new Point(0.5, 0.5);
    private static final Point INLAND_ALGERIA = new Point(3.0, 28.0);
    private static final Point OPEN_NORTH_ATLANTIC = new Point(-40.0, 30.0);

    @Container static final RadarContainers containers = new RadarContainers();

    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        try {
            browser = playwright.chromium().launch();
        } catch (RuntimeException launchFailure) {
            playwright.close();
            throw launchFailure;
        }
    }

    @AfterAll
    static void closeBrowser() {
        try {
            if (browser != null) {
                browser.close();
            }
        } finally {
            if (playwright != null) {
                playwright.close();
            }
        }
    }

    @BeforeEach
    void startWithoutSquares() throws Exception {
        containers.clearSquareCounts();
    }

    @Test
    void pageShowsAMapOfTheFullWorldWidth() throws Exception {
        try (var session = Session.start()) {
            session.open();

            waitUntil(session.page, MAP_LOADED);
            assertThat(session.page.evaluate("window.map.getBounds().getWest()"))
                    .asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                    .isLessThanOrEqualTo(-180.0);
            assertThat(session.page.evaluate("window.map.getBounds().getEast()"))
                    .asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                    .isGreaterThanOrEqualTo(180.0);
        }
    }

    @Test
    void pageShowsNoSquareWhenTheEndpointReturnsNone() throws Exception {
        try (var session = Session.start()) {
            session.open();

            waitUntil(session.page, SQUARES_LOADED);
            assertThat(squaresRenderedAt(session.page, SQUARE_52N_13E_CENTRE)).isZero();
            assertThat(squaresRenderedAt(session.page, SQUARE_48N_2E_CENTRE)).isZero();
        }
    }

    @Test
    void squaresAreGreyFromLightForOneAircraftToDarkestForTheHighestCount(@TempDir Path dir)
            throws Exception {
        var file = HeatmapFixture.writeBerlinAndParis(dir.resolve("heatmap.bin.ttf"));

        try (var session = Session.start()) {
            session.importAndOpen(file, 2);

            waitUntil(session.page, SQUARES_LOADED);
            var screenshot = MapScreenshot.of(session.page);
            assertThat(screenshot.colourAt(SQUARE_52N_13E_CENTRE))
                    .as("fill of 52N 13E, 2 aircraft, the highest count")
                    .isEqualTo(DARK_GREY);
            assertThat(screenshot.colourAt(SQUARE_48N_2E_CENTRE))
                    .as("fill of 48N 2E, 1 aircraft")
                    .isEqualTo(LIGHT_GREY);
            assertThat(squaresRenderedAt(session.page, SQUARE_0N_0E_CENTRE))
                    .as("squares at 0N 0E, which has no aircraft")
                    .isZero();
        }
    }

    @Test
    void aSquareWithNoAircraftIsWhiteWhileASquareWithAircraftIsGrey() throws Exception {
        try (var session = Session.start()) {
            session.page.route(
                    "**/api/squares",
                    route ->
                            route.fulfill(
                                    new Route.FulfillOptions()
                                            .setContentType("application/json")
                                            .setBody(
                                                    """
                                                    [{"latitude":52,"longitude":13,"aircraft":0},
                                                     {"latitude":48,"longitude":2,"aircraft":2}]
                                                    """)));
            session.open();

            waitUntil(session.page, SQUARES_LOADED);
            var screenshot = MapScreenshot.of(session.page);
            assertThat(squaresRenderedAt(session.page, SQUARE_52N_13E_CENTRE))
                    .as("squares drawn at 52N 13E, which has 0 aircraft")
                    .isPositive();
            assertThat(screenshot.colourAt(SQUARE_52N_13E_CENTRE))
                    .as("fill of 52N 13E, 0 aircraft")
                    .isEqualTo(Color.WHITE);
            assertThat(screenshot.colourAt(SQUARE_48N_2E_CENTRE))
                    .as("fill of 48N 2E, 2 aircraft")
                    .isEqualTo(DARK_GREY);
        }
    }

    @Test
    void theOnlySquareIsDarkestWhenTheHighestCountIsOne(@TempDir Path dir) throws Exception {
        var file = HeatmapFixture.writeOneAircraftInBerlin(dir.resolve("heatmap.bin.ttf"));

        try (var session = Session.start()) {
            session.importAndOpen(file, 1);

            waitUntil(session.page, SQUARES_LOADED);
            assertThat(MapScreenshot.of(session.page).colourAt(SQUARE_52N_13E_CENTRE))
                    .isEqualTo(DARK_GREY);
        }
    }

    @Test
    void styleHasBaseMapLayersWhiteFillsAndOnePixelLines() throws Exception {
        try (var session = Session.start()) {
            session.open();

            waitUntil(session.page, SQUARES_LOADED);
            assertThat(session.page.evaluate("window.map.getStyle().layers.map((l) => l.id)"))
                    .as("layer ids in drawing order")
                    .isEqualTo(LAYER_IDS);
            assertThat(paintProperty(session.page, "background", "background-color"))
                    .as("background colour")
                    .isEqualTo(WHITE);
            assertThat(paintProperty(session.page, "water", "fill-color"))
                    .as("water colour")
                    .isEqualTo(WHITE);
            assertThat(paintProperty(session.page, COASTLINE_LAYER, "line-color"))
                    .as("coastline colour")
                    .isEqualTo(LINE_GREY);
            assertThat(paintProperty(session.page, COUNTRY_BORDERS_LAYER, "line-color"))
                    .as("country border colour")
                    .isEqualTo(LINE_GREY);
            assertThat(paintProperty(session.page, COASTLINE_LAYER, "line-width"))
                    .as("coastline width")
                    .isEqualTo(LINE_WIDTH);
            assertThat(paintProperty(session.page, COUNTRY_BORDERS_LAYER, "line-width"))
                    .as("country border width")
                    .isEqualTo(LINE_WIDTH);
            assertThat(filter(session.page, COASTLINE_LAYER))
                    .as("coastline filter: outline of ocean water")
                    .isEqualTo(List.of("==", List.of("get", "class"), "ocean"));
            assertThat(filter(session.page, COUNTRY_BORDERS_LAYER))
                    .as("country border filter: admin level 2, not maritime")
                    .isEqualTo(
                            List.of(
                                    "all",
                                    List.of("==", List.of("get", "admin_level"), 2),
                                    List.of("==", List.of("get", "maritime"), 0)));
            assertThat(session.page.evaluate("window.map.getStyle().layers.map((l) => l.type)"))
                    .as("layer types")
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .doesNotContain("symbol");
        }
    }

    @Test
    void baseMapDrawsCoastlinesAndBordersAndLeavesLandAndOpenOceanWhite(@TempDir Path dir)
            throws Exception {
        var file = HeatmapFixture.writeBerlinAndParis(dir.resolve("heatmap.bin.ttf"));

        try (var session = Session.start()) {
            session.importAndOpen(file, 2);

            waitUntil(session.page, SQUARES_LOADED);
            waitUntil(session.page, BASE_MAP_LOADED);
            var screenshot = MapScreenshot.of(session.page);
            screenshot.saveTo(SCREENSHOT);

            assertThat(featuresRendered(session.page, COASTLINE_LAYER))
                    .as("rendered coastline features")
                    .isPositive();
            assertThat(featuresRendered(session.page, COUNTRY_BORDERS_LAYER))
                    .as("rendered country border features")
                    .isPositive();
            assertThat(screenshot.colourAt(INLAND_ALGERIA))
                    .as("land far from borders and squares")
                    .isEqualTo(Color.WHITE);
            assertThat(screenshot.colourAt(OPEN_NORTH_ATLANTIC))
                    .as("open ocean")
                    .isEqualTo(Color.WHITE);
        }
    }

    @Test
    void pageLoadsScriptsAndStylesheetsOnlyFromTheServiceItself() throws Exception {
        var scriptAndStylesheetUrls = new CopyOnWriteArrayList<String>();

        try (var session = Session.start()) {
            session.page.onRequest(
                    request -> {
                        if (SCRIPT_AND_STYLESHEET.contains(request.resourceType())) {
                            scriptAndStylesheetUrls.add(request.url());
                        }
                    });
            session.open();

            waitUntil(session.page, SQUARES_LOADED);
            assertThat(scriptAndStylesheetUrls)
                    .isNotEmpty()
                    .allSatisfy(
                            requestUrl ->
                                    assertThat(requestUrl).startsWith(session.origin() + "/"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/other", "/maplibre/unknown.mjs", "/maplibre/", "/api/squares/other"})
    void unknownPathsAreNotFound(String path) throws Exception {
        try (var server = MapServer.start(containers.settings(), 0);
                var http = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create(url(server.port(), path))).build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(404);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/api/squares"})
    void onlyGetIsAllowed(String path) throws Exception {
        try (var server = MapServer.start(containers.settings(), 0);
                var http = HttpClient.newHttpClient()) {
            var request =
                    HttpRequest.newBuilder(URI.create(url(server.port(), path)))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(405);
            assertThat(response.headers().firstValue("Allow")).hasValue("GET");
        }
    }

    private static final class Session implements AutoCloseable {

        private final RadarService service;
        private final Page page;

        static Session start() throws Exception {
            var service = RadarService.start(containers.settings(), 0);
            try {
                return new Session(service, browser.newPage());
            } catch (RuntimeException pageFailure) {
                service.close();
                throw pageFailure;
            }
        }

        private Session(RadarService service, Page page) {
            this.service = service;
            this.page = page;
        }

        String origin() {
            return url(service.port(), "");
        }

        void open() {
            page.navigate(url(service.port(), "/"));
        }

        void importAndOpen(Path file, int expectedSquares) throws Exception {
            SquaresClient.importAndAwait(
                    file, containers.settings(), service.port(), expectedSquares);
            open();
        }

        @Override
        public void close() throws Exception {
            try {
                page.close();
            } finally {
                service.close();
            }
        }
    }

    private static String url(int port, String path) {
        return "http://localhost:" + port + path;
    }

    private static Object paintProperty(Page page, String layer, String property) {
        return page.evaluate(
                "([layer, property]) => window.map.getPaintProperty(layer, property)",
                List.of(layer, property));
    }

    private static Object filter(Page page, String layer) {
        return page.evaluate("(layer) => window.map.getFilter(layer)", layer);
    }

    private static void waitUntil(Page page, String condition) {
        try {
            page.waitForFunction(
                    condition, null, new Page.WaitForFunctionOptions().setTimeout(WAIT_TIMEOUT_MS));
        } catch (PlaywrightException timeout) {
            throw new AssertionError(
                    "Not true within %.0f ms: %s".formatted(WAIT_TIMEOUT_MS, condition), timeout);
        }
    }

    private static int squaresRenderedAt(Page page, Point point) {
        return (Integer)
                page.evaluate(
                        """
                        ([longitude, latitude]) =>
                            window.map.queryRenderedFeatures(
                                window.map.project([longitude, latitude]),
                                { layers: ['%s'] }).length
                        """
                                .formatted(SQUARES_LAYER),
                        List.of(point.longitude(), point.latitude()));
    }

    private static int featuresRendered(Page page, String layer) {
        return (Integer)
                page.evaluate(
                        "(layer) => window.map.queryRenderedFeatures({ layers: [layer] }).length",
                        layer);
    }
}
