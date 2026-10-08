package io.github.corioliskraft.doomsdayradar;

import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startable;

final class RadarContainers implements Startable {

    private static final int SCHEMA_REGISTRY_PORT = 8081;
    private static final String KAFKA_INTERNAL_LISTENER = "kafka:19092";

    private final Network network = Network.newNetwork();

    private final KafkaContainer kafka =
            new KafkaContainer("apache/kafka:4.3.1")
                    .withNetwork(network)
                    .withListener(KAFKA_INTERNAL_LISTENER);

    private final GenericContainer<?> schemaRegistry =
            new GenericContainer<>("confluentinc/cp-schema-registry:8.3.2")
                    .withNetwork(network)
                    .withExposedPorts(SCHEMA_REGISTRY_PORT)
                    .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                    .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + SCHEMA_REGISTRY_PORT)
                    .withEnv(
                            "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                            "PLAINTEXT://" + KAFKA_INTERNAL_LISTENER)
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_TIMEOUT_MS", "10000")
                    .dependsOn(kafka)
                    .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    private final ClickHouseContainer clickHouse =
            new ClickHouseContainer("clickhouse/clickhouse-server:26.3");

    Settings settings() {
        return new Settings(
                kafka.getBootstrapServers(),
                "http://"
                        + schemaRegistry.getHost()
                        + ":"
                        + schemaRegistry.getMappedPort(SCHEMA_REGISTRY_PORT),
                clickHouse.getHttpUrl(),
                clickHouse.getUsername(),
                clickHouse.getPassword());
    }

    void clearSquareCounts() throws Exception {
        try (var client = ClickHouse.connect(settings())) {
            client.execute("TRUNCATE TABLE IF EXISTS " + Tables.SQUARE_COUNTS).get();
        }
    }

    @Override
    public void start() {
        kafka.start();
        schemaRegistry.start();
        clickHouse.start();
    }

    @Override
    public void stop() {
        clickHouse.stop();
        schemaRegistry.stop();
        kafka.stop();
        network.close();
    }
}
