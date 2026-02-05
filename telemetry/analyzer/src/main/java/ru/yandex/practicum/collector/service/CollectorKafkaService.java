package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.SensorEvent;
import ru.yandex.practicum.grpc.telemetry.event.HubEvent;

@Service
@RequiredArgsConstructor
public class CollectorKafkaService {

    private final KafkaProducer<String, byte[]> producer;

    public void sendSensor(SensorEvent event) {
        producer.send(new ProducerRecord<>(
                "telemetry.sensors.v1",
                event.getHubId(),
                event.toByteArray()
        ));
    }

    public void sendHub(HubEvent event) {
        producer.send(new ProducerRecord<>(
                "telemetry.hubs.v1",
                event.getHubId(),
                event.toByteArray()
        ));
    }
}
