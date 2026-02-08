package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.entity.ConditionType;
import ru.yandex.practicum.analyzer.entity.Scenario;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.kafka.telemetry.event.SnapshotAvro;

import java.util.List;

@SuppressWarnings("unused")
@Service
@RequiredArgsConstructor
public class ScenarioEngine {

    private final ScenarioRepository scenarioRepo;
    private final HubRouterService hubRouter;

    public void processSnapshot(SnapshotAvro snap){

        List<Scenario> scenarios =
                scenarioRepo.findByHubId(
                        snap.getHubId().toString());

        scenarios.stream()
                .filter(s -> matches(snap,s))
                .forEach(s -> execute(snap,s));
    }

    private boolean matches(
            SnapshotAvro snap,
            Scenario s){

        return s.getConditions()
                .stream()
                .allMatch(c -> {

                    Integer val =
                            extractValue(snap,c.getType());

                    return switch(c.getOperation()){
                        case GREATER -> val>c.getValue();
                        case LESS -> val<c.getValue();
                        case EQUAL -> val.equals(c.getValue());
                    };
                });
    }

    private void execute(
            SnapshotAvro snap,
            Scenario s){

        s.getActions().forEach(a ->
                hubRouter.sendAction(
                        snap.getHubId().toString(),
                        s.getName(),
                        a.getType(),
                        a.getValue()));
    }

    private Integer extractValue(
            SnapshotAvro snap,
            ConditionType type){

        // тут можно распарсить payload
        return 20;
    }
}
