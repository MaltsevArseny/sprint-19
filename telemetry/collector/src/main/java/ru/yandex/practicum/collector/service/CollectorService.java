package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.dto.SensorEvent;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.collector.util.AvroSerializer;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class CollectorService {

    private final Producer<String, byte[]> producer;
    private final SensorEventMapper mapper;

    @Value("${collector.topics.sensors}")
    private String sensorsTopic;

    public void collectSensor(SensorEvent event) {

        // 1️⃣ DTO -> Avro
        SensorEventAvro avro = mapper.toAvro(event);

        // 2️⃣ Avro -> byte[]
        byte[] payload = AvroSerializer.serialize(avro);

        // 3️⃣ Формируем record
        ProducerRecord<String, byte[]> record =
                new ProducerRecord<>(
                        sensorsTopic,
                        null,
                        event.getTimestamp().toEpochMilli(), // timestamp события
                        event.getHubId(), // ключ = hubId
                        payload
                );

        // 4️⃣ Отправка + callback
        producer.send(record, (metadata, exception) -> {

            if (exception != null) {
                log.error("Ошибка отправки в Kafka", exception);
            } else {
                log.info(
                        "Отправлено в Kafka: topic={}, partition={}, offset={}",
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset()
                );
            }
        });
    }
}
