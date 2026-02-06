package ru.yandex.practicum.analyzer.processor;

import org.apache.kafka.clients.consumer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.SnapshotAvro;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@SuppressWarnings("unused")
public class SnapshotProcessor implements Runnable {

    private static final Logger log =
            LoggerFactory.getLogger(SnapshotProcessor.class);

    private final KafkaConsumer<String, SnapshotAvro> consumer;

    private volatile boolean running = true;

    public SnapshotProcessor(Properties props,
                             ScenarioRepository scenarioRepository,
                             SensorRepository sensorRepository) {

        this.consumer = new KafkaConsumer<>(props);

        consumer.subscribe(List.of("snapshots"));
    }

    @Override
    public void run() {
        try {
            while (running) {

                ConsumerRecords<String, SnapshotAvro> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, SnapshotAvro> record : records) {
                    processSnapshot(record.value());
                }

                consumer.commitSync();
            }

        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Closing consumer");
                consumer.close();
            }
        }
    }

    private void processSnapshot(SnapshotAvro snapshot) {
        log.info("Snapshot received for hub: {}", snapshot.getHubId());

        // TODO: логика проверки сценариев
    }

    public void shutdown() {
        running = false;
        consumer.wakeup();
    }
}
