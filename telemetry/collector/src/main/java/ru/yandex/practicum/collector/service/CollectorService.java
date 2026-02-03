package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.dto.SensorEvent;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Service
@RequiredArgsConstructor
public class CollectorService {

    private final KafkaTemplate<String, Object> kafka;
    private final SensorEventMapper mapper;

    public void collectSensor(SensorEvent event) {
        SensorEventAvro avro = mapper.toAvro(event);
        kafka.send("telemetry.sensors.v1", event.getId(), avro);
    }
}
