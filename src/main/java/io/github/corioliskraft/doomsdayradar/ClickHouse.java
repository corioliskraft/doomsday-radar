package io.github.corioliskraft.doomsdayradar;

import com.clickhouse.client.api.Client;

final class ClickHouse {

    static Client connect(Settings settings) {
        return new Client.Builder()
                .addEndpoint(settings.clickHouseUrl())
                .setUsername(settings.clickHouseUser())
                .setPassword(settings.clickHousePassword())
                .build();
    }

    private ClickHouse() {}
}
