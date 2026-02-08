package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.entity.*;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Service
@RequiredArgsConstructor
@Deprecated // Помечаем как устаревший
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;

    public List<Action> getActionsForSnapshot(String hubId, Map<String, Double> sensorValues) {
        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        List<Action> result = new ArrayList<>();

        for (Scenario scenario : scenarios) {
            boolean allMatch = scenario.getConditions().stream()
                    .allMatch(c -> checkCondition(c, sensorValues));

            if (allMatch) {
                result.addAll(scenario.getActions());
            }
        }

        return result;
    }

    private boolean checkCondition(Condition condition, Map<String, Double> sensorValues) {
        // Для этого метода нужна логика сопоставления sensorType с sensorId
        // Временная реализация
        return false;
    }
}