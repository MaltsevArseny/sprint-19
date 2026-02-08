package ru.yandex.practicum.aggregator.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SnapshotAvro;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AggregatorProcessor {

    public SnapshotAvro process(SensorEventAvro event) {

        Map<CharSequence, ByteBuffer> sensors = new HashMap<>();

        sensors.put(
                event.getId(),
                ByteBuffer.allocate(0)
        );

        return SnapshotAvro.newBuilder()
                .setHubId(event.getHubId())
                .setSensors(sensors)
                .build();
    }
}
