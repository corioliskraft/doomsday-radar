package io.github.corioliskraft.doomsdayradar;

public record Settings(
        String kafkaBootstrapServers,
        String schemaRegistryUrl,
        String clickHouseUrl,
        String clickHouseUser,
        String clickHousePassword) {}
