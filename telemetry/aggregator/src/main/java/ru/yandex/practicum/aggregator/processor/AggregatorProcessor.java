package ru.yandex.practicum.aggregator.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SnapshotAvro;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@Component
@Slf4j
public class AggregatorProcessor {

    // Временное хранилище для снапшотов по хабам
    private final Map<String, Map<String, ByteBuffer>> snapshots = new HashMap<>();

    public SnapshotAvro process(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();

        Map<String, ByteBuffer> hubSnapshot = snapshots.computeIfAbsent(hubId, k -> new HashMap<>());

        double value = extractValueFromEvent(event);
        ByteBuffer oldBuffer = hubSnapshot.get(sensorId);

        // Проверяем, изменилось ли значение
        if (oldBuffer != null) {
            // Создаем копию буфера для чтения
            ByteBuffer readBuffer = oldBuffer.duplicate();
            readBuffer.rewind();
            double oldValue = readBuffer.getDouble();

            if (Math.abs(oldValue - value) < 0.001) {
                return null; // Значение не изменилось
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(8).putDouble(value);
        buffer.flip();
        hubSnapshot.put(sensorId, buffer);

        log.debug("Updated snapshot for hub: {}, sensor: {}, value: {}", hubId, sensorId, value);

        return SnapshotAvro.newBuilder()
                .setHubId(hubId)
                .setSensors(hubSnapshot)
                .setTimestamp(Instant.ofEpochMilli(event.getTimestamp()))
                .build();
    }

    private double extractValueFromEvent(SensorEventAvro event) {
        Object payload = event.getPayload();

        if (payload instanceof ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro climate) {
            return climate.getTemperatureC();
        } else if (payload instanceof ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro temp) {
            return temp.getTemperatureC();
        } else if (payload instanceof ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro light) {
            return light.getLuminosity();
        } else if (payload instanceof ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro motion) {
            return motion.getMotion() ? 1.0 : 0.0;
        } else if (payload instanceof ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro sw) {
            return sw.getState() ? 1.0 : 0.0;
        }

        log.warn("Unknown payload type: {}", payload != null ? payload.getClass().getName() : "null");
        return 0.0;
    }
}