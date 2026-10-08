package io.github.corioliskraft.doomsdayradar;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public final class RadarService implements AutoCloseable {

    private final SquareCountConsumer consumer;
    private final MapServer server;

    public static RadarService start(Settings settings, int port)
            throws IOException, InterruptedException, ExecutionException {
        var consumer = SquareCountConsumer.start(settings);
        try {
            return new RadarService(consumer, MapServer.start(settings, port));
        } catch (Exception e) {
            try {
                consumer.close();
            } catch (Exception closeFailure) {
                if (closeFailure instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    private RadarService(SquareCountConsumer consumer, MapServer server) {
        this.consumer = consumer;
        this.server = server;
    }

    public int port() {
        return server.port();
    }

    @Override
    public void close() throws InterruptedException {
        try {
            server.close();
        } finally {
            consumer.close();
        }
    }
}
