package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${kafka.topics.hubs}")
    private String hubsTopic;

    public void sendSensorEvent(SensorEventAvro event) {
        kafkaTemplate.send(sensorsTopic, event.getHubId().toString(), event);
    }

    public void sendHubEvent(HubEventAvro event) {
        kafkaTemplate.send(hubsTopic, event.getHubId().toString(), event);
    }
}

