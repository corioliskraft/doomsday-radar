package io.github.corioliskraft.doomsdayradar;

import static io.github.corioliskraft.doomsdayradar.HeatmapFixture.position;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Testcontainers
class OneFileIT {

    static final Network network = Network.newNetwork();

    @Container
    static final KafkaContainer kafka =
            new KafkaContainer("apache/kafka:4.3.1")
                    .withNetwork(network)
                    .withListener("kafka:19092");

    @Container
    static final GenericContainer<?> schemaRegistry =
            new GenericContainer<>("confluentinc/cp-schema-registry:8.3.2")
                    .withNetwork(network)
                    .withExposedPorts(8081)
                    .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                    .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                    .withEnv(
                            "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                            "PLAINTEXT://kafka:19092")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_TIMEOUT_MS", "10000")
                    .dependsOn(kafka)
                    .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    @Container
    static final ClickHouseContainer clickHouse =
            new ClickHouseContainer("clickhouse/clickhouse-server:26.3");

    @Test
    void endpointReturnsTheAircraftCountOfEachSquareInOneFile(@TempDir Path dir) throws Exception {
        var file =
                HeatmapFixture.write(
                        dir.resolve("heatmap.bin.ttf"),
                        position(0xA00001, 52.52, 13.40),
                        position(0xA00002, 52.10, 13.90),
                        position(0xA00003, 48.85, 2.35));
        var settings =
                new Settings(
                        kafka.getBootstrapServers(),
                        "http://"
                                + schemaRegistry.getHost()
                                + ":"
                                + schemaRegistry.getMappedPort(8081),
                        clickHouse.getHttpUrl(),
                        clickHouse.getUsername(),
                        clickHouse.getPassword());

        try (var service = RadarService.start(settings, 0)) {
            ImportJob.run(file, settings);

            assertThat(squaresWhenTwoArrive(service.port()))
                    .containsExactlyInAnyOrder(
                            Map.of("latitude", 52, "longitude", 13, "aircraft", 2),
                            Map.of("latitude", 48, "longitude", 2, "aircraft", 1));
        }
    }

    private static List<Map<String, Object>> squaresWhenTwoArrive(int port) throws Exception {
        var http = HttpClient.newHttpClient();
        var request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/squares"))
                        .build();
        var deadline = Instant.now().plus(Duration.ofSeconds(60));
        List<Map<String, Object>> squares;
        do {
            Thread.sleep(500);
            var body = http.send(request, HttpResponse.BodyHandlers.ofString()).body();
            squares = JsonMapper.shared().readValue(body, new TypeReference<>() {});
        } while (squares.size() < 2 && Instant.now().isBefore(deadline));
        return squares;
    }
}
