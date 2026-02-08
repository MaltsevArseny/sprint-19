package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.entity.*;
import ru.yandex.practicum.analyzer.repository.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final SensorRepository sensorRepository;
    private final HubRepository hubRepository;
    private final ScenarioRepository scenarioRepository;

    private volatile boolean running = true;

    @Override
    public void run() {
        try {
            consumer.subscribe(List.of("telemetry.hubs.v1"));
            log.info("HubEventProcessor started, subscribed to telemetry.hubs.v1");

            while (running) {
                ConsumerRecords<String, HubEventAvro> records =
                        consumer.poll(Duration.ofMillis(1000));

                if (!records.isEmpty()) {
                    log.debug("Polled {} hub events", records.count());
                }

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    try {
                        handleEvent(record.value());
                    } catch (Exception e) {
                        log.error("Error processing hub event at offset {}: {}",
                                record.offset(), e.getMessage(), e);
                    }
                }
            }
        } catch (WakeupException ignored) {
            log.info("HubEventProcessor received wakeup signal");
        } catch (Exception e) {
            log.error("Fatal error in HubEventProcessor: {}", e.getMessage(), e);
        } finally {
            consumer.close();
            log.info("HubEventProcessor stopped");
        }
    }

    @Transactional
    protected void handleEvent(HubEventAvro event) {
        if (event == null || event.getPayload() == null) {
            log.warn("Received null event or payload");
            return;
        }

        String hubId = event.getHubId();
        Object payload = event.getPayload();

        log.debug("Processing hub event: hubId={}, payloadType={}",
                hubId, payload.getClass().getSimpleName());

        // Используем switch вместо if-else
        switch (payload) {
            case DeviceAddedEventAvro added -> handleDeviceAdded(hubId, added);
            case DeviceRemovedEventAvro removed -> handleDeviceRemoved(hubId, removed);
            case HubCreatedEventAvro created -> handleHubCreated(hubId, created);
            case HubRemovedEventAvro removed -> handleHubRemoved(removed);
            case ScenarioAddedEventAvro added -> handleScenarioAdded(hubId, added);
            case ScenarioUpdatedEventAvro updated -> handleScenarioUpdated(hubId, updated);
            case ScenarioRemovedEventAvro removed -> handleScenarioRemoved(hubId, removed);
            default -> log.warn("Unsupported event type for hub {}: {}",
                    hubId, payload.getClass().getSimpleName());
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro added) {
        String deviceId = added.getId();
        String deviceType = mapDeviceTypeToSensorType(added.getType());

        // Сначала проверяем существование хаба
        Hub hub = hubRepository.findById(hubId).orElseGet(() -> {
            Hub newHub = new Hub();
            newHub.setId(hubId);
            newHub.setName("Hub_" + hubId);
            newHub.setLocation("Unknown");
            return hubRepository.save(newHub);
        });

        // Проверяем существование сенсора
        Optional<Sensor> existingSensor = sensorRepository.findByIdAndHubId(deviceId, hubId);

        if (existingSensor.isPresent()) {
            // Обновляем существующий сенсор
            Sensor sensor = existingSensor.get();
            sensor.setType(deviceType);
            sensor.setLastUpdated(Instant.now());
            sensorRepository.save(sensor);
            log.debug("Updated existing device: id={}, hub={}, type={}",
                    deviceId, hubId, deviceType);
        } else {
            // Создаем новый сенсор
            Sensor sensor = new Sensor();
            sensor.setId(deviceId);
            sensor.setHubId(hubId);
            sensor.setHub(hub);
            sensor.setType(deviceType);
            sensor.setName("Device_" + deviceId);
            sensor.setLastUpdated(Instant.now());
            sensorRepository.save(sensor);
            log.info("Device added: id={}, hub={}, type={}",
                    deviceId, hubId, deviceType);
        }
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro removed) {
        String deviceId = removed.getId();

        sensorRepository.findByIdAndHubId(deviceId, hubId).ifPresent(sensor -> {
            sensorRepository.delete(sensor);
            log.info("Device removed: hub={}, id={}", hubId, deviceId);
        });
    }

    private void handleHubCreated(String hubId, HubCreatedEventAvro created) {
        if (!hubRepository.existsById(hubId)) {
            Hub hub = new Hub();
            hub.setId(hubId);

            String hubName = created.getName();
            if (hubName == null) {
                hubName = "Hub_" + hubId;
            }
            hub.setName(hubName);

            String location = created.getLocation();
            if (location == null) {
                location = "Unknown";
            }
            hub.setLocation(location);

            hubRepository.save(hub);
            log.info("Hub created: id={}, name={}", hubId, hub.getName());
        } else {
            log.debug("Hub already exists: {}", hubId);
        }
    }

    private void handleHubRemoved(HubRemovedEventAvro removed) {
        String hubId = removed.getHubId();

        hubRepository.findById(hubId).ifPresent(hub -> {
            hubRepository.delete(hub);
            log.info("Hub removed: {}", hubId);
        });
    }

    @Transactional
    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro added) {
        String scenarioId = added.getScenarioId();
        String scenarioName = added.getName();

        // Проверяем существование хаба
        if (!hubRepository.existsById(hubId)) {
            log.warn("Cannot add scenario to non-existent hub: {}", hubId);
            return;
        }

        // Проверяем дубликаты
        Optional<Scenario> existingScenario = scenarioRepository.findByScenarioId(scenarioId);
        if (existingScenario.isPresent()) {
            log.debug("Scenario already exists: {}", scenarioId);
            return;
        }

        // Создаем новый сценарий
        Scenario scenario = new Scenario();
        scenario.setScenarioId(scenarioId);
        scenario.setHubId(hubId);
        scenario.setName(scenarioName);
        scenario.setActive(added.getActive());

        scenarioRepository.save(scenario);
        log.info("Scenario added: id={}, hub={}, name={}",
                scenarioId, hubId, scenarioName);
    }

    @Transactional
    private void handleScenarioUpdated(String hubId, ScenarioUpdatedEventAvro updated) {
        String scenarioId = updated.getScenarioId();

        scenarioRepository.findByScenarioId(scenarioId).ifPresent(scenario -> {
            if (!scenario.getHubId().equals(hubId)) {
                log.warn("Scenario {} belongs to different hub: expected {}, actual {}",
                        scenarioId, hubId, scenario.getHubId());
                return;
            }

            // Обновляем только изменившиеся поля
            if (updated.getName() != null) {
                scenario.setName(updated.getName());
            }
            // Для boolean используем Boolean объект для проверки на null
            boolean active = updated.getActive();
            scenario.setActive(active);

            scenarioRepository.save(scenario);
            log.info("Scenario updated: id={}, hub={}", scenarioId, hubId);
        });
    }

    @Transactional
    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro removed) {
        String scenarioId = removed.getScenarioId();

        scenarioRepository.findByScenarioId(scenarioId).ifPresent(scenario -> {
            if (!scenario.getHubId().equals(hubId)) {
                log.warn("Scenario {} belongs to different hub: expected {}, actual {}",
                        scenarioId, hubId, scenario.getHubId());
                return;
            }

            scenarioRepository.delete(scenario);
            log.info("Scenario removed: id={}, hub={}", scenarioId, hubId);
        });
    }

    private String mapDeviceTypeToSensorType(DeviceTypeAvro deviceType) {
        return switch (deviceType) {
            case TEMPERATURE_SENSOR -> "temperature";
            case CLIMATE_SENSOR -> "climate";
            case LIGHT_SENSOR -> "light";
            case MOTION_SENSOR -> "motion";
            case SWITCH_SENSOR -> "switch";
        };
    }

    public void shutdown() {
        log.info("Shutting down HubEventProcessor...");
        running = false;
        consumer.wakeup();
    }
}