package io.github.corioliskraft.doomsdayradar;

import com.clickhouse.client.api.Client;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

final class SquareCountConsumer implements AutoCloseable {

    private final KafkaConsumer<String, SquareCount> consumer;
    private final Client clickHouse;
    private final Thread pollLoop;

    static SquareCountConsumer start(Settings settings)
            throws InterruptedException, ExecutionException {
        AvroClasses.trustOwnClasses();
        createTopic(settings);
        var clickHouse = ClickHouse.connect(settings);
        clickHouse
                .execute(
                        """
                        CREATE TABLE IF NOT EXISTS square_counts (
                            latitude Int16,
                            longitude Int16,
                            aircraft UInt32
                        ) ENGINE = MergeTree ORDER BY (latitude, longitude)\
                        """)
                .get();
        return new SquareCountConsumer(new KafkaConsumer<>(consumerConfig(settings)), clickHouse);
    }

    private SquareCountConsumer(KafkaConsumer<String, SquareCount> consumer, Client clickHouse) {
        this.consumer = consumer;
        this.clickHouse = clickHouse;
        this.pollLoop = Thread.ofPlatform().name("square-count-consumer").start(this::poll);
    }

    private void poll() {
        try {
            consumer.subscribe(List.of(Topics.SQUARE_COUNTS));
            while (true) {
                var records = consumer.poll(Duration.ofMillis(1000));
                if (!records.isEmpty()) {
                    insert(
                            StreamSupport.stream(records.spliterator(), false)
                                    .map(ConsumerRecord::value)
                                    .toList());
                    consumer.commitSync();
                }
            }
        } catch (WakeupException _) {
            // close() stops the loop.
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        } finally {
            consumer.close();
        }
    }

    private void insert(List<SquareCount> counts) throws InterruptedException, ExecutionException {
        var rows =
                counts.stream()
                        .map(
                                count ->
                                        "(%d, %d, %d)"
                                                .formatted(
                                                        count.getLatitude(),
                                                        count.getLongitude(),
                                                        count.getAircraft()))
                        .collect(Collectors.joining(", "));
        clickHouse
                .execute("INSERT INTO square_counts (latitude, longitude, aircraft) VALUES " + rows)
                .get();
    }

    @Override
    public void close() throws InterruptedException {
        consumer.wakeup();
        pollLoop.join();
        clickHouse.close();
    }

    private static void createTopic(Settings settings)
            throws InterruptedException, ExecutionException {
        try (var admin =
                Admin.create(
                        Map.of(
                                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                                settings.kafkaBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(Topics.SQUARE_COUNTS, 1, (short) 1)))
                    .all()
                    .get();
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw e;
            }
        }
    }

    private static Map<String, Object> consumerConfig(Settings settings) {
        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                settings.kafkaBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "square-count-consumer",
                ConsumerConfig.GROUP_PROTOCOL_CONFIG,
                "consumer",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                KafkaAvroDeserializer.class.getName(),
                "schema.registry.url",
                settings.schemaRegistryUrl(),
                "specific.avro.reader",
                true);
    }
}
