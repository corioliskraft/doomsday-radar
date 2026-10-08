package io.github.corioliskraft.doomsdayradar;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public final class ImportJob {

    public static void run(Path file, Settings settings)
            throws IOException, RestClientException, InterruptedException, ExecutionException {
        AvroClasses.trustOwnClasses();
        var counts = HeatmapFile.countPerSquare(file);
        registerSchema(settings);
        try (var producer = new KafkaProducer<String, SquareCount>(producerConfig(settings))) {
            for (var count : counts) {
                var key = count.getLatitude() + "," + count.getLongitude();
                producer.send(new ProducerRecord<>(Topics.SQUARE_COUNTS, key, count)).get();
            }
        }
    }

    private static void registerSchema(Settings settings) throws IOException, RestClientException {
        try (var registry = new CachedSchemaRegistryClient(settings.schemaRegistryUrl(), 10)) {
            registry.register(
                    Topics.SQUARE_COUNTS + "-value", new AvroSchema(SquareCount.getClassSchema()));
        }
    }

    private static Map<String, Object> producerConfig(Settings settings) {
        return Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                settings.kafkaBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaAvroSerializer.class.getName(),
                "schema.registry.url",
                settings.schemaRegistryUrl(),
                "auto.register.schemas",
                false,
                "use.latest.version",
                true);
    }

    private ImportJob() {}
}
