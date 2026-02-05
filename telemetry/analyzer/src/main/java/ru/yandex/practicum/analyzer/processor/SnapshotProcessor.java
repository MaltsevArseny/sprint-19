package ru.yandex.practicum.analyzer.processor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.KafkaProps;
import ru.yandex.practicum.analyzer.config.TopicProps;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@SuppressWarnings("unused")
@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotProcessor implements Runnable {

    private final KafkaProps kafkaProps;
    private final TopicProps topicProps;

    private volatile boolean running = true;
    private KafkaConsumer<String, SensorsSnapshotAvro> consumer;

    // автозапуск потока
    @PostConstruct
    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProps.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-snapshots");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        props.put("schema.registry.url",
                kafkaProps.getSchemaRegistry());
        props.put("specific.avro.reader", true);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        consumer = new KafkaConsumer<>(props);

        try {
            consumer.subscribe(
                    Collections.singleton(topicProps.getSnapshots()));

            while (running) {

                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, SensorsSnapshotAvro> r : records) {

                    SensorsSnapshotAvro snapshot = r.value();

                    log.info("Snapshot from hub {} devices={}",
                            snapshot.getHubId(),
                            snapshot.getSensors().size());

                    for (SensorEventAvro s : snapshot.getSensors()) {
                        log.debug("Sensor {} timestamp {}",
                                s.getId(),
                                s.getTimestamp());
                    }
                }

                // фиксируем offset вручную
                consumer.commitSync();
            }

        } catch (WakeupException ignored) {
            log.info("SnapshotProcessor shutting down");
        } finally {
            consumer.close();
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (consumer != null) {
            consumer.wakeup();
        }
    }
}
