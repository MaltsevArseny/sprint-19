package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@SuppressWarnings("unused")
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectorKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSensorEvent(SensorEventAvro event) {
        kafkaTemplate.send("telemetry.sensors.v1",
                event.getHubId().toString(),
                event);
    }

    public void sendHubEvent(HubEventAvro event) {
        kafkaTemplate.send("telemetry.hubs.v1",
                event.getHubId().toString(),
                event);
    }
}

