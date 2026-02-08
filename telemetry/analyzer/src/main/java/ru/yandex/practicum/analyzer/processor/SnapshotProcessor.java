package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.analyzer.service.ScenarioEvaluationService;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor implements Runnable {

    private final KafkaConsumer<String, SnapshotAvro> consumer;
    private final ScenarioEvaluationService scenarioService;
    private final SensorRepository sensorRepository;

    private volatile boolean running = true;

    @Override
    public void run() {
        try {
            consumer.subscribe(List.of("telemetry.snapshots.v1"));
            log.info("SnapshotProcessor started, subscribed to telemetry.snapshots.v1");

            while (running) {
                ConsumerRecords<String, SnapshotAvro> records =
                        consumer.poll(Duration.ofMillis(1000));

                if (!records.isEmpty()) {
                    log.debug("Polled {} snapshot records", records.count());
                }

                for (ConsumerRecord<String, SnapshotAvro> record : records) {
                    try {
                        handleSnapshot(record.value());
                        consumer.commitSync();
                        log.debug("Processed snapshot at offset: {}", record.offset());
                    } catch (Exception e) {
                        log.error("Error processing snapshot at offset {}: {}",
                                record.offset(), e.getMessage(), e);
                    }
                }
            }
        } catch (WakeupException ignored) {
            log.info("SnapshotProcessor received wakeup signal");
        } catch (Exception e) {
            log.error("Fatal error in SnapshotProcessor: {}", e.getMessage(), e);
        } finally {
            consumer.close();
            log.info("SnapshotProcessor stopped");
        }
    }

    @Transactional
    protected void handleSnapshot(SnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        Map<String, Double> sensorValues = extractSensorValues(snapshot);

        // Исправлено: getTimestamp() возвращает Instant
        Instant snapshotTime = snapshot.getTimestamp();

        log.debug("Processing snapshot for hub: {}, sensor count: {}, timestamp: {}",
                hubId, sensorValues.size(), snapshotTime);

        updateSensorValues(hubId, sensorValues);
        scenarioService.evaluateScenarios(hubId, sensorValues);
    }

    @Transactional
    protected void updateSensorValues(String hubId, Map<String, Double> sensorValues) {
        sensorValues.forEach((sensorId, value) ->
                sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresentOrElse(
                        sensor -> {
                            sensor.setLastValue(value);
                            sensor.setLastUpdated(Instant.now());
                            sensorRepository.save(sensor);
                            log.trace("Updated sensor: hub={}, id={}, value={}", hubId, sensorId, value);
                        },
                        () -> log.warn("Sensor not found: hub={}, id={}", hubId, sensorId)
                )
        );
    }

    private Map<String, Double> extractSensorValues(SnapshotAvro snapshot) {
        Map<String, Double> values = new HashMap<>();

        snapshot.getSensors().forEach((sensorId, bufferObj) -> {
            try {
                Double value = extractValue(bufferObj);
                if (value != null && !Double.isNaN(value)) {
                    values.put(sensorId, value);
                }
            } catch (Exception e) {
                log.warn("Failed to extract value for sensor {}: {}", sensorId, e.getMessage());
            }
        });

        return values;
    }

    private Double extractValue(Object buffer) {
        if (buffer == null) {
            return null;
        }

        try {
            return switch (buffer) {
                case ByteBuffer byteBuffer -> extractValueFromByteBuffer(byteBuffer);
                case byte[] bytes -> extractValueFromBytes(bytes);
                case CharSequence charSequence -> {
                    try {
                        yield Double.parseDouble(charSequence.toString());
                    } catch (NumberFormatException e) {
                        log.warn("Cannot parse string as double: {}", charSequence);
                        yield null;
                    }
                }
                default -> {
                    log.warn("Unsupported buffer type: {}", buffer.getClass().getName());
                    yield null;
                }
            };
        } catch (Exception e) {
            log.warn("Error extracting value: {}", e.getMessage());
            return null;
        }
    }

    private Double extractValueFromByteBuffer(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }

        ByteBuffer bufferCopy = buffer.duplicate();

        try {
            bufferCopy.rewind();

            if (bufferCopy.remaining() == 0) {
                return null;
            }

            byte[] bytes = new byte[bufferCopy.remaining()];
            bufferCopy.get(bytes);

            Double avroValue = tryDeserializeAvro(bytes);
            if (avroValue != null) {
                return avroValue;
            }

            bufferCopy.rewind();

            int remaining = bufferCopy.remaining();
            return switch (remaining) {
                case 8 -> bufferCopy.getDouble();
                case 4 -> (double) bufferCopy.getFloat();
                case 2 -> (double) bufferCopy.getShort();
                case 1 -> (double) bufferCopy.get();
                default -> {
                    if (remaining > 8) {
                        byte[] doubleBytes = new byte[8];
                        bufferCopy.get(doubleBytes);
                        ByteBuffer doubleBuffer = ByteBuffer.wrap(doubleBytes);
                        yield doubleBuffer.getDouble();
                    } else {
                        yield interpretNonStandardBytes(bytes);
                    }
                }
            };

        } catch (Exception e) {
            log.warn("Failed to extract from ByteBuffer: {}", e.getMessage());
            return null;
        }
    }

    private Double extractValueFromBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        try {
            if (bytes.length == 0) {
                return null;
            }

            Double avroValue = tryDeserializeAvro(bytes);
            if (avroValue != null) {
                return avroValue;
            }

            ByteBuffer buffer = ByteBuffer.wrap(bytes);

            return switch (bytes.length) {
                case 8 -> buffer.getDouble();
                case 4 -> (double) buffer.getFloat();
                case 2 -> (double) buffer.getShort();
                case 1 -> (double) buffer.get();
                default -> interpretNonStandardBytes(bytes);
            };

        } catch (Exception e) {
            log.warn("Failed to extract from bytes (length {}): {}", bytes.length, e.getMessage());
            return null;
        }
    }

    private Double interpretNonStandardBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            switch (bytes.length) {
                case 3:
                    int value24 = ((bytes[0] & 0xFF) << 16) | ((bytes[1] & 0xFF) << 8) | (bytes[2] & 0xFF);
                    return (double) value24;
                case 5:
                case 6:
                case 7:
                    return readAsPaddedDouble(bytes);
                default:
                    if (bytes.length > 8) {
                        ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, 8);
                        return buffer.getDouble();
                    }
                    log.warn("Unsupported byte array length: {}", bytes.length);
                    return null;
            }
        } catch (Exception e) {
            log.warn("Failed to interpret non-standard bytes (length {}): {}", bytes.length, e.getMessage());
            return null;
        }
    }

    private Double readAsPaddedDouble(byte[] bytes) {
        byte[] paddedBytes = new byte[8];
        System.arraycopy(bytes, 0, paddedBytes, 0, bytes.length);
        ByteBuffer paddedBuffer = ByteBuffer.wrap(paddedBytes);
        return paddedBuffer.getDouble();
    }

    private Double tryDeserializeAvro(byte[] bytes) {
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(input, null);

            SpecificDatumReader<ClimateSensorAvro> climateReader =
                    new SpecificDatumReader<>(ClimateSensorAvro.getClassSchema());
            ClimateSensorAvro climate = climateReader.read(null, decoder);
            return (double) climate.getTemperatureC();

        } catch (Exception e1) {
            try {
                ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(input, null);

                SpecificDatumReader<TemperatureSensorAvro> tempReader =
                        new SpecificDatumReader<>(TemperatureSensorAvro.getClassSchema());
                TemperatureSensorAvro temp = tempReader.read(null, decoder);
                return (double) temp.getTemperatureC();

            } catch (Exception e2) {
                try {
                    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                    BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(input, null);

                    SpecificDatumReader<LightSensorAvro> lightReader =
                            new SpecificDatumReader<>(LightSensorAvro.getClassSchema());
                    LightSensorAvro light = lightReader.read(null, decoder);
                    return (double) light.getLuminosity();

                } catch (Exception e3) {
                    try {
                        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(input, null);

                        SpecificDatumReader<MotionSensorAvro> motionReader =
                                new SpecificDatumReader<>(MotionSensorAvro.getClassSchema());
                        MotionSensorAvro motion = motionReader.read(null, decoder);
                        return motion.getMotion() ? 1.0 : 0.0;

                    } catch (Exception e4) {
                        try {
                            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(input, null);

                            SpecificDatumReader<SwitchSensorAvro> switchReader =
                                    new SpecificDatumReader<>(SwitchSensorAvro.getClassSchema());
                            SwitchSensorAvro switchSensor = switchReader.read(null, decoder);
                            return switchSensor.getState() ? 1.0 : 0.0;

                        } catch (Exception e5) {
                            return null;
                        }
                    }
                }
            }
        }
    }

    public void shutdown() {
        log.info("Shutting down SnapshotProcessor...");
        running = false;
        consumer.wakeup();
    }
}