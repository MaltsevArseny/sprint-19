package ru.yandex.practicum.analyzer.processor;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.practicum.analyzer.entity.Sensor;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;

import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@SuppressWarnings("unused")
public class HubEventProcessor implements Runnable {

    private static final Logger log =
            LoggerFactory.getLogger(HubEventProcessor.class);

    private final KafkaConsumer<String, Object> consumer;
    private final SensorRepository sensorRepository;

    private volatile boolean running = true;

    public HubEventProcessor(Properties props,
                             SensorRepository sensorRepository,
                             ScenarioRepository scenarioRepository) {

        this.consumer = new KafkaConsumer<>(props);
        this.sensorRepository = sensorRepository;

        consumer.subscribe(Collections.singletonList("hub-events"));
    }

    @Override
    public void run() {

        try {
            while (running) {

                ConsumerRecords<String, Object> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, Object> record : records) {
                    handleEvent(record.value());
                }

                consumer.commitSync();
            }

        } catch (WakeupException e) {
            log.info("Shutdown signal");
        } finally {

            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }

    private void handleEvent(Object event) {

        if (event instanceof SensorEventAvro sensorEvent) {

            Sensor sensor = new Sensor();

            sensor.setId((String) sensorEvent.getId());
            sensor.setHubId((String) sensorEvent.getHubId());

            sensorRepository.save(sensor);

            log.info("Saved sensor {}", sensor.getId());
        }
    }

    public void shutdown() {
        running = false;
        consumer.wakeup();
    }
}
