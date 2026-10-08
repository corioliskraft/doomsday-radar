package io.github.corioliskraft.doomsdayradar;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

final class SquaresClient {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    static List<Map<String, Object>> importAndAwait(
            Path file, Settings settings, int port, int minimumSquares) throws Exception {
        ImportJob.run(file, settings);
        return awaitAtLeast(port, minimumSquares);
    }

    static List<Map<String, Object>> awaitAtLeast(int port, int minimumSquares) throws Exception {
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/squares"))
                        .timeout(REQUEST_TIMEOUT)
                        .build();
        var deadline = Instant.now().plus(POLL_TIMEOUT);
        List<Map<String, Object>> squares = poll(request);
        while (squares.size() < minimumSquares && Instant.now().isBefore(deadline)) {
            Thread.sleep(POLL_INTERVAL);
            squares = poll(request);
        }
        if (squares.size() < minimumSquares) {
            throw new AssertionError(
                    "Expected at least %d squares within %s but the endpoint returned %d"
                            .formatted(minimumSquares, POLL_TIMEOUT, squares.size()));
        }
        return squares;
    }

    private static List<Map<String, Object>> poll(HttpRequest request) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var body = http.send(request, HttpResponse.BodyHandlers.ofString()).body();
            return JsonMapper.shared().readValue(body, new TypeReference<>() {});
        } catch (HttpTimeoutException _) {
            return List.of();
        }
    }

    private SquaresClient() {}
}
