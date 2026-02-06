package ru.yandex.practicum.analyzer.processor;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class SnapshotProcessor implements Runnable {

    private static final Logger log =
            LoggerFactory.getLogger(SnapshotProcessor.class);

    private final KafkaConsumer<String, HubEventAvro> consumer;

    private volatile boolean running = true;

    public SnapshotProcessor(Properties props) {
        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("snapshots"));
    }

    @Override
    public void run() {

        try {
            while (running) {

                ConsumerRecords<String, HubEventAvro> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    log.info("Event received for hub: {}",
                            record.value().getHubId());
                }

                consumer.commitSync();
            }

        } catch (WakeupException e) {
            log.info("Shutdown signal received");

        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }

    public void shutdown() {
        running = false;
        consumer.wakeup();
    }
}
