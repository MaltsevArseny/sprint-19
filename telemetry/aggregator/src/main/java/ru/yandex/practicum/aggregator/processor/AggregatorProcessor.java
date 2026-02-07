package ru.yandex.practicum.aggregator.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@Component
@RequiredArgsConstructor
public class AggregatorProcessor {

    private final KafkaTemplate<String, SnapshotAvro> kafkaTemplate;

    private final Map<String, SnapshotAvro> snapshots = new HashMap<>();

    @KafkaListener(topics = "${kafka.topics.sensors}")
    public void process(SensorEventAvro event) {

        String hubId = (String) event.getHubId();

        SnapshotAvro snapshot = snapshots.getOrDefault(
                hubId,
                SnapshotAvro.newBuilder()
                        .setHubId(hubId)
                        .setSensors(new HashMap<>())
                        .build()
        );

        byte[] newPayload = event.getPayload().toString().getBytes();

        byte[] old = snapshot.getSensors().get(event.getId()).array();

        snapshot.getSensors().put(
                event.getId(),
                ByteBuffer.wrap(newPayload)
        );

        snapshots.put(hubId, snapshot);

        kafkaTemplate.send(
                "telemetry.snapshots.v1",
                hubId,
                snapshot
        );
    }
}
