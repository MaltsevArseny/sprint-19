package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.dto.SensorEvent;
import ru.yandex.practicum.collector.dto.hub.HubEvent;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.collector.mapper.HubEventMapper;
import ru.yandex.practicum.collector.util.AvroSerializer;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Service
@RequiredArgsConstructor
public class CollectorService {

    private final Producer<String, byte[]> producer;
    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;

    @Value("${collector.topics.sensors}")
    private String sensorsTopic;

    @Value("${collector.topics.hubs}")
    private String hubsTopic;

    // ✅ SENSOR EVENTS
    public void collectSensorEvent(SensorEvent event) {

        SensorEventAvro avro = sensorEventMapper.toAvro(event);
        byte[] payload = AvroSerializer.serialize(avro);

        ProducerRecord<String, byte[]> record =
                new ProducerRecord<>(
                        sensorsTopic,
                        null,
                        event.getTimestamp().toEpochMilli(),
                        event.getHubId(),
                        payload
                );

        producer.send(record);
        producer.flush();
    }

    // ✅ HUB EVENTS
    public void collectHubEvent(HubEvent event) {

        HubEventAvro avro = hubEventMapper.toAvro(event);
        byte[] payload = AvroSerializer.serialize(avro);

        ProducerRecord<String, byte[]> record =
                new ProducerRecord<>(
                        hubsTopic,
                        null,
                        event.getTimestamp().toEpochMilli(),
                        event.getHubId(),
                        payload
                );

        producer.send(record);
        producer.flush();
    }
}

