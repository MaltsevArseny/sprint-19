package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.entity.Sensor;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final SensorRepository sensorRepository;

    @Value("${kafka.hubTopic}")
    private String topic;

    private volatile boolean running = true;

    @Override
    public void run() {

        consumer.subscribe(List.of(topic));

        try {
            while (running) {

                var records = consumer.poll(Duration.ofMillis(500));

                for (var record : records) {
                    handleEvent(record.value());
                }

                consumer.commitSync();
            }

        } catch (WakeupException ignored) {
        } finally {
            consumer.close();
        }
    }

    private void handleEvent(HubEventAvro event) {

        // В HubEventAvro payload — это union!
        if (event.getPayload() instanceof DeviceAddedEventAvro added) {

            Sensor sensor = new Sensor();
            sensor.setId(added.getId().toString());
            sensor.setHubId(event.getHubId().toString());

            sensorRepository.save(sensor);

            log.info("Sensor saved {}", sensor.getId());
        }
    }

    public void shutdown() {
        running = false;
        consumer.wakeup();
    }
}
