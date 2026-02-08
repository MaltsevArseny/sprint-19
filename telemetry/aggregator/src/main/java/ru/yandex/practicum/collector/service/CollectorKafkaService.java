package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.config.TopicProps;
import ru.yandex.practicum.kafka.telemetry.event.*;

@SuppressWarnings("unused")
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectorKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TopicProps topics;

    public void sendSensorEvent(SensorEventAvro event) {
        kafkaTemplate.send(
                topics.getSensors(),
                event.getHubId(),
                event
        );
        log.info("Sensor event sent");
    }

    public void sendHubEvent(HubEventAvro event) {
        kafkaTemplate.send(
                topics.getHubs(),
                event.getHubId(),
                event
        );
        log.info("Hub event sent");
    }
}
