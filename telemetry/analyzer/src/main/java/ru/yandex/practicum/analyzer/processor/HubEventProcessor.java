package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.yandex.practicum.analyzer.entity.Sensor;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.*;

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

                ConsumerRecords<String, HubEventAvro> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, HubEventAvro> record : records) {
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

        if (event == null || event.getPayload() == null) {
            return;
        }

        String hubId = event.getHubId().toString();
        Object payload = event.getPayload();

        if (payload instanceof DeviceAddedEventAvro added) {
            handleDeviceAdded(hubId, added);

        } else if (payload instanceof DeviceRemovedEventAvro removed) {
            handleDeviceRemoved(removed);
        }
    }

    // ---------- DEVICE ADDED ----------

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro e) {

        String deviceId = e.getId().toString();

        if (sensorRepository.existsById(deviceId)) {
            log.info("Device already exists {}", deviceId);
            return;
        }

        Sensor sensor = new Sensor();
        sensor.setId(deviceId);
        sensor.setHubId(hubId);

        sensorRepository.save(sensor);

        log.info("Device added {}", deviceId);
    }

    // ---------- DEVICE REMOVED ----------

    private void handleDeviceRemoved(DeviceRemovedEventAvro e) {

        String deviceId = e.getId().toString();

        if (!sensorRepository.existsById(deviceId)) {
            return;
        }

        sensorRepository.deleteById(deviceId);

        log.info("Device removed {}", deviceId);
    }

    // ---------- SHUTDOWN ----------

    public void shutdown() {
        running = false;
        consumer.wakeup();
    }
}
