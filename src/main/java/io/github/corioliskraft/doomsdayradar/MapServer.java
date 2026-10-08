package io.github.corioliskraft.doomsdayradar;

import com.clickhouse.client.api.Client;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

final class MapServer implements AutoCloseable {

    private final HttpServer http;
    private final Client clickHouse;

    static MapServer start(Settings settings, int port) throws IOException {
        var server =
                new MapServer(
                        HttpServer.create(new InetSocketAddress(port), 0),
                        ClickHouse.connect(settings));
        server.http.start();
        return server;
    }

    private MapServer(HttpServer http, Client clickHouse) {
        this.http = http;
        this.clickHouse = clickHouse;
        http.createContext("/api/squares", this::squares);
    }

    int port() {
        return http.getAddress().getPort();
    }

    private void squares(HttpExchange exchange) throws IOException {
        var json =
                clickHouse
                        .queryAll("SELECT latitude, longitude, aircraft FROM square_counts")
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
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, json.length);
        try (var body = exchange.getResponseBody()) {
            body.write(json);
        }
    }

    @Override
    public void close() {
        http.stop(0);
        clickHouse.close();
    }
}
