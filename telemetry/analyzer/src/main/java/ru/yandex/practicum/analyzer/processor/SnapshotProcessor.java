package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.KafkaProps;
import ru.yandex.practicum.analyzer.config.TopicProps;
import ru.yandex.practicum.analyzer.service.ScenarioService;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotProcessor implements Runnable {

    private final KafkaProps kafkaProps;
    private final TopicProps topicProps;
    private final ScenarioService scenarioService;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private KafkaConsumer<String, byte[]> consumer;

    @Override
    public void run() {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProps.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-snapshots");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try {
            consumer = new KafkaConsumer<>(props);

            consumer.subscribe(
                    Collections.singleton(topicProps.getSnapshots()));

            while (running.get()) {

                ConsumerRecords<String, byte[]> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, byte[]> r : records) {

                    String hubId = r.key();

                    log.info("Snapshot for hub {}", hubId);

                    scenarioService.findByHub(hubId)
                            .forEach(s ->
                                    log.info("Scenario found: {}", s.getName()));
                }

                consumer.commitSync();
            }

        } catch (WakeupException e) {
            log.info("SnapshotProcessor stopping...");
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    public void shutdown() {
        running.set(false);
        if (consumer != null) {
            consumer.wakeup();
        }
    }
}
