package ru.yandex.practicum.analyzer.service;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.entity.*;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.hubrouter.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.hubrouter.DeviceActionRequest;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioEvaluationService {

    @GrpcClient("hub-router")
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;

    private final Map<String, List<Scenario>> scenarioCache = new HashMap<>();
    private final Map<String, Long> scenarioCacheTimestamp = new HashMap<>();
    private static final long CACHE_TTL_MS = 30000;

    @Transactional
    public void evaluateScenarios(String hubId, Map<String, Double> sensorValues) {
        log.debug("Evaluating scenarios for hub: {}, sensor values count: {}", hubId, sensorValues.size());

        List<Scenario> scenarios = getCachedScenarios(hubId);
        if (scenarios.isEmpty()) {
            log.debug("No active scenarios for hub: {}", hubId);
            return;
        }

        log.debug("Found {} active scenarios for hub: {}", scenarios.size(), hubId);

        Map<String, String> sensorTypes = getSensorTypes(hubId);
        Map<String, Double> allSensorValues = new HashMap<>(sensorValues);
        enrichSensorValues(hubId, allSensorValues);

        scenarios.sort((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()));

        List<CompletableFuture<Void>> executionFutures = new ArrayList<>();

        for (Scenario scenario : scenarios) {
            if (areAllConditionsMet(scenario, allSensorValues, sensorTypes)) {
                log.info("Scenario conditions met: hub={}, scenario={}", hubId, scenario.getName());
                CompletableFuture<Void> future = executeScenarioAsync(scenario, hubId, allSensorValues);
                executionFutures.add(future);
            }
        }

        if (!executionFutures.isEmpty()) {
            CompletableFuture.allOf(executionFutures.toArray(new CompletableFuture[0]))
                    .exceptionally(ex -> {
                        log.error("Error in scenario executions: {}", ex.getMessage(), ex);
                        return null;
                    });

            log.info("Triggered {} scenarios for hub: {}", executionFutures.size(), hubId);
        }
    }

    private List<Scenario> getCachedScenarios(String hubId) {
        long now = System.currentTimeMillis();
        Long lastUpdate = scenarioCacheTimestamp.get(hubId);

        if (lastUpdate != null && (now - lastUpdate) < CACHE_TTL_MS) {
            return scenarioCache.get(hubId);
        }

        List<Scenario> scenarios = scenarioRepository.findByHubIdAndActiveTrue(hubId);
        scenarioCache.put(hubId, scenarios);
        scenarioCacheTimestamp.put(hubId, now);

        log.debug("Updated scenario cache for hub: {}, scenarios: {}", hubId, scenarios.size());
        return scenarios;
    }

    private Map<String, String> getSensorTypes(String hubId) {
        return sensorRepository.findByHubId(hubId).stream()
                .collect(Collectors.toMap(Sensor::getId, Sensor::getType, (v1, v2) -> v1));
    }

    private void enrichSensorValues(String hubId, Map<String, Double> sensorValues) {
        sensorRepository.findByHubId(hubId).forEach(sensor -> {
            if (!sensorValues.containsKey(sensor.getId()) && sensor.getLastValue() != null) {
                sensorValues.put(sensor.getId(), sensor.getLastValue());
            }
        });
    }

    private boolean areAllConditionsMet(Scenario scenario,
                                        Map<String, Double> sensorValues,
                                        Map<String, String> sensorTypes) {

        List<Condition> conditions = scenario.getConditions();
        if (conditions.isEmpty()) {
            return false;
        }

        boolean finalResult = true;
        boolean isFirstCondition = true;

        for (Condition condition : conditions) {
            boolean conditionResult = evaluateCondition(condition, sensorValues, sensorTypes);

            if (isFirstCondition) {
                finalResult = conditionResult;
                isFirstCondition = false;
            } else {
                Condition.LogicalOperator operator = condition.getLogicalOperator();
                if (operator == Condition.LogicalOperator.AND) {
                    finalResult = finalResult && conditionResult;
                    if (!finalResult) {
                        return false;
                    }
                } else {
                    finalResult = finalResult || conditionResult;
                    if (finalResult) {
                        return true;
                    }
                }
            }
        }

        return finalResult;
    }

    private boolean evaluateCondition(Condition condition,
                                      Map<String, Double> sensorValues,
                                      Map<String, String> sensorTypes) {

        if (condition.getSensorId() != null && !condition.getSensorId().isEmpty()) {
            Double currentValue = sensorValues.get(condition.getSensorId());
            if (currentValue == null) {
                log.debug("Sensor {} not found for condition", condition.getSensorId());
                return false;
            }
            return condition.isMet(currentValue);
        }

        List<String> matchingSensorIds = sensorTypes.entrySet().stream()
                .filter(e -> condition.getSensorType().equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        if (matchingSensorIds.isEmpty()) {
            log.debug("No sensors of type {} found", condition.getSensorType());
            return false;
        }

        return matchingSensorIds.stream()
                .anyMatch(sensorId -> {
                    Double currentValue = sensorValues.get(sensorId);
                    return currentValue != null && condition.isMet(currentValue);
                });
    }

    @Async
    protected CompletableFuture<Void> executeScenarioAsync(Scenario scenario,
                                                           String hubId,
                                                           Map<String, Double> sensorValues) {
        return CompletableFuture.runAsync(() -> {
            try {
                executeScenario(scenario, hubId, sensorValues);
            } catch (Exception e) {
                log.error("Failed to execute scenario {}: {}", scenario.getName(), e.getMessage(), e);
            }
        });
    }

    @Transactional
    protected void executeScenario(Scenario scenario, String hubId, Map<String, Double> sensorValues) {
        log.info("Executing scenario: hub={}, name={}, actions={}",
                hubId, scenario.getName(), scenario.getActions().size());

        saveScenarioExecution(scenario, hubId, sensorValues);

        scenario.getActions().forEach(action -> {
            try {
                if (action.getDelayMs() != null && action.getDelayMs() > 0) {
                    Thread.sleep(action.getDelayMs());
                }
                sendDeviceActionWithRetry(hubId, scenario.getName(), action);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting to execute action: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Failed to execute action: {}", e.getMessage(), e);
            }
        });

        log.info("Scenario executed successfully: hub={}, scenario={}", hubId, scenario.getName());
    }

    private void saveScenarioExecution(Scenario scenario, String hubId,
                                       Map<String, Double> sensorValues) {
        log.debug("Scenario execution: hub={}, scenario={}, sensorValues={}",
                hubId, scenario.getName(), sensorValues.size());
    }

    private void sendDeviceActionWithRetry(String hubId, String scenarioName, Action action) {
        int maxRetries = 3;
        long baseDelayMs = 1000L;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                sendDeviceAction(hubId, scenarioName, action);
                log.debug("Action sent successfully on attempt {}/{}", attempt, maxRetries);
                return;

            } catch (StatusRuntimeException e) {
                Status.Code code = e.getStatus().getCode();

                if ((code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED)
                        && attempt < maxRetries) {

                    long delay = baseDelayMs * (long) Math.pow(2, attempt - 1);
                    log.warn("gRPC error, retrying in {}ms (attempt {}/{})", delay, attempt, maxRetries);

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("Failed to send action after {} attempts: {}",
                            attempt, e.getStatus().getDescription(), e);
                    break;
                }

            } catch (Exception e) {
                log.error("Unexpected error sending action on attempt {}: {}", attempt, e.getMessage(), e);
                break;
            }
        }
    }

    private void sendDeviceAction(String hubId, String scenarioName, Action action) {
        DeviceActionProto deviceAction = DeviceActionProto.newBuilder()
                .setType(action.getDeviceType())
                .setValue(action.getValue() != null ? action.getValue() : 0)
                .build();

        DeviceActionRequest request = DeviceActionRequest.newBuilder()
                .setHubId(hubId)
                .setScenarioName(scenarioName)
                .setAction(deviceAction)
                .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                .build();

        try {
            // Вызываем gRPC метод
            hubRouterClient
                    .withDeadlineAfter(10, TimeUnit.SECONDS)
                    .handleDeviceAction(request);

            log.info("Device action sent: hub={}, scenario={}, device={}, value={}",
                    hubId, scenarioName, action.getDeviceType(), action.getValue());

        } catch (StatusRuntimeException e) {
            log.error("gRPC error sending device action: {}", e.getStatus().getDescription());
            throw e;
        }
    }
}