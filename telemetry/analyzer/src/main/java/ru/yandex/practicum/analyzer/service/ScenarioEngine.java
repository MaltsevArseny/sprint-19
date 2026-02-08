package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.entity.*;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Deprecated
public class ScenarioEngine {

    private final ScenarioRepository scenarioRepo;
    private final HubRouterService hubRouter;

    public void processSnapshot(ru.yandex.practicum.kafka.telemetry.event.SnapshotAvro snap) {
        List<Scenario> scenarios = scenarioRepo.findByHubId(snap.getHubId());

        scenarios.stream()
                .filter(s -> matches(snap, s))
                .forEach(s -> execute(snap, s));
    }

    private boolean matches(ru.yandex.practicum.kafka.telemetry.event.SnapshotAvro snap, Scenario s) {
        return s.getConditions().stream()
                .allMatch(c -> {
                    double value = 20.0; // Заглушка

                    return switch (c.getOperation()) {
                        case GREATER -> value > c.getThresholdValue();
                        case LESS -> value < c.getThresholdValue();
                        case EQUAL -> Math.abs(value - c.getThresholdValue()) < 0.001;
                        case NOT_EQUAL -> Math.abs(value - c.getThresholdValue()) > 0.001;
                        case BETWEEN -> {
                            Double value2 = c.getThresholdValue2();
                            yield value2 != null && value >= c.getThresholdValue() && value <= value2;
                        }
                    };
                });
    }

    private void execute(ru.yandex.practicum.kafka.telemetry.event.SnapshotAvro snap, Scenario s) {
        s.getActions().forEach(a -> hubRouter.sendAction(
                snap.getHubId(),
                s.getName(),
                a.getType().name(),
                a.getValue() != null ? a.getValue() : 0
        ));
    }
}