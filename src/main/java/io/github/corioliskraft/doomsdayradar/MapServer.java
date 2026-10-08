package io.github.corioliskraft.doomsdayradar;

import com.clickhouse.client.api.Client;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

final class MapServer implements AutoCloseable {

    private static final String HTML = "text/html; charset=utf-8";
    private static final String JAVASCRIPT = "text/javascript; charset=utf-8";
    private static final String CSS = "text/css; charset=utf-8";
    private static final String TEXT = "text/plain; charset=utf-8";
    private static final String JSON = "application/json";
    private static final String SQUARES_PATH = "/api/squares";

    private static final Map<String, AssetSource> ASSET_SOURCES =
            Map.of(
                    "/", new AssetSource("/web/index.html", HTML),
                    "/maplibre/maplibre-gl.mjs",
                            new AssetSource("/web/maplibre/maplibre-gl.mjs", JAVASCRIPT),
                    "/maplibre/maplibre-gl-shared.mjs",
                            new AssetSource("/web/maplibre/maplibre-gl-shared.mjs", JAVASCRIPT),
                    "/maplibre/maplibre-gl-worker.mjs",
                            new AssetSource("/web/maplibre/maplibre-gl-worker.mjs", JAVASCRIPT),
                    "/maplibre/maplibre-gl.css",
                            new AssetSource("/web/maplibre/maplibre-gl.css", CSS),
                    "/maplibre/LICENSE.txt", new AssetSource("/web/maplibre/LICENSE.txt", TEXT));

    private record AssetSource(String resource, String contentType) {}

    private record Asset(String contentType, byte[] bytes) {}

    private final HttpServer http;
    private final Client clickHouse;
    private final Map<String, Asset> assets;

    static MapServer start(Settings settings, int port) throws IOException {
        var assets = readAssets();
        var clickHouse = ClickHouse.connect(settings);
        try {
            var server =
                    new MapServer(
                            HttpServer.create(new InetSocketAddress(port), 0), clickHouse, assets);
            server.http.start();
            return server;
        } catch (IOException | RuntimeException failure) {
            clickHouse.close();
            throw failure;
        }
    }

    private MapServer(HttpServer http, Client clickHouse, Map<String, Asset> assets) {
        this.http = http;
        this.clickHouse = clickHouse;
        this.assets = assets;
        http.createContext("/", getOnly(this::serveAsset));
        http.createContext(SQUARES_PATH, getOnly(this::serveSquares));
    }

    int port() {
        return http.getAddress().getPort();
    }

    private static Map<String, Asset> readAssets() throws IOException {
        var assets = new HashMap<String, Asset>();
        for (var entry : ASSET_SOURCES.entrySet()) {
            var source = entry.getValue();
            assets.put(
                    entry.getKey(),
                    new Asset(source.contentType(), readResource(source.resource())));
        }
        return Map.copyOf(assets);
    }

    private static byte[] readResource(String resourceName) throws IOException {
        try (var resource = MapServer.class.getResourceAsStream(resourceName)) {
            if (resource == null) {
                throw new IOException("Missing class path resource " + resourceName);
            }
            return resource.readAllBytes();
        }
    }

    private static HttpHandler getOnly(HttpHandler handler) {
        return exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                respondEmpty(exchange, 405);
                return;
            }
            handler.handle(exchange);
        };
    }

    private void serveAsset(HttpExchange exchange) throws IOException {
        var asset = assets.get(exchange.getRequestURI().getPath());
        if (asset == null) {
            respondEmpty(exchange, 404);
            return;
        }
        respond(exchange, asset.contentType(), asset.bytes());
    }

    private void serveSquares(HttpExchange exchange) throws IOException {
        if (!SQUARES_PATH.equals(exchange.getRequestURI().getPath())) {
            respondEmpty(exchange, 404);
            return;
        }
        var json =
                clickHouse
                        .queryAll(
                                "SELECT latitude, longitude, aircraft FROM "
                                        + Tables.SQUARE_COUNTS)
                        .stream()
                        .map(
                                row ->
                                        "{\"latitude\":%d,\"longitude\":%d,\"aircraft\":%d}"
                                                .formatted(
                                                        row.getInteger("latitude"),
                                                        row.getInteger("longitude"),
                                                        row.getLong("aircraft")))
                        .collect(Collectors.joining(",", "[", "]"))
                        .getBytes(StandardCharsets.UTF_8);
        respond(exchange, JSON, json);
    }

    private static void respondEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private static void respond(HttpExchange exchange, String contentType, byte[] bytes)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var body = exchange.getResponseBody()) {
            body.write(bytes);
        }
    }

    @Override
    public void close() {
        http.stop(0);
        clickHouse.close();
    }
}
