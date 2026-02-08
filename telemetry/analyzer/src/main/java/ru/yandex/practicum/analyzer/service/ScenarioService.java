package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import ru.yandex.practicum.analyzer.entity.*;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;

    public List<Action> getActionsForSnapshot(
            String hubId,
            Map<ConditionType, Integer> sensorValues) {

        List<Scenario> scenarios =
                scenarioRepository.findByHubId(hubId);

        List<Action> result = new ArrayList<>();

        for (Scenario scenario : scenarios) {

            boolean allMatch =
                    scenario.getConditions()
                            .stream()
                            .allMatch(c ->
                                    checkCondition(c, sensorValues));

            if (allMatch) {
                result.addAll(scenario.getActions());
            }
        }

        return result;
    }

    private boolean checkCondition(
            Condition condition,
            Map<ConditionType, Integer> sensorValues) {

        Integer actual =
                sensorValues.get(condition.getType());

        if (actual == null) return false;

        int target = condition.getValue();

        switch (condition.getOperation()) {

            case GREATER:
                return actual > target;

            case LESS:
                return actual < target;

            case EQUAL:
                return actual == target;

            default:
                return false;
        }
    }
}
