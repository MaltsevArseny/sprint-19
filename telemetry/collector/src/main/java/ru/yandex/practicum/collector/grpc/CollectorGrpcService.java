package ru.yandex.practicum.collector.grpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.service.CollectorKafkaService;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorGrpcService {

    private final CollectorKafkaService kafkaService;

    // Временные методы для обработки событий
    public void handleSensorEvent(String sensorId, String hubId, long timestamp) {
        SensorEventAvro avro = SensorEventAvro.newBuilder()
                .setId(sensorId)
                .setHubId(hubId)
                .setTimestamp(timestamp)
                .build();

        kafkaService.sendSensorEvent(avro);
        log.info("Processed sensor event: sensorId={}, hubId={}", sensorId, hubId);
    }

    public void handleHubEvent(String hubId, String deviceId, long timestamp) {
        HubEventAvro avro = HubEventAvro.newBuilder()
                .setHubId(hubId)
                .setTimestamp(timestamp)
                .build();

        kafkaService.sendHubEvent(avro);
        log.info("Processed hub event: hubId={}, deviceId={}", hubId, deviceId);
    }
}