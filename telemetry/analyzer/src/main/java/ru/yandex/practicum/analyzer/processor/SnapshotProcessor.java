package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.entity.ConditionType;
import ru.yandex.practicum.analyzer.service.HubRouterService;
import ru.yandex.practicum.analyzer.service.ScenarioService;
import ru.yandex.practicum.kafka.telemetry.event.SnapshotAvro;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor implements Runnable {

    private final KafkaConsumer<String, SnapshotAvro> consumer;
    private final ScenarioService scenarioService;
    private final HubRouterService hubRouterService;

    @Value("${kafka.snapshotTopic}")
    private String topic;

    private volatile boolean running = true;

    @Override
    public void run() {

        consumer.subscribe(List.of(topic));

        try {
            while (running) {

                var records =
                        consumer.poll(Duration.ofMillis(500));

                for (var record : records) {
                    handleSnapshot(record.value());
                }

                consumer.commitSync();
            }

        } catch (WakeupException ignored) {
        } finally {
            consumer.close();
        }
    }

    private void handleSnapshot(SnapshotAvro snapshot) {

        Map<ConditionType, Integer> values = new HashMap<>();

        snapshot.getSensors().forEach((sensorId, buffer) -> {

            ByteBuffer bb = (ByteBuffer) buffer;

            int value = bb.getInt(0);

            values.put(
                    ConditionType.valueOf(sensorId.toString()),
                    value
            );
        });

        var actions =
                scenarioService.getActionsForSnapshot(
                        snapshot.getHubId().toString(),
                        values
                );

        actions.forEach(a ->
                hubRouterService.sendAction(
                        snapshot.getHubId().toString(),
                        a.getScenario().getName(),
                        a.getType(),
                        a.getValue() == null ? 0 : a.getValue()
                ));
    }

    public void shutdown() {
        running = false;
        consumer.wakeup();
    }
}
