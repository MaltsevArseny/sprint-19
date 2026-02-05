package ru.yandex.practicum.analyzer.processor;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.KafkaProps;
import ru.yandex.practicum.analyzer.config.TopicProps;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class HubEventProcessor implements Runnable {

    private final KafkaProps kafkaProps;
    private final TopicProps topicProps;

    private final Map<String, Set<String>> hubDevices = new HashMap<>();

    private volatile boolean running = true;
    private KafkaConsumer<String, HubEventAvro> consumer;

    @Override
    public void run() {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProps.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-hubs");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        props.put("schema.registry.url", kafkaProps.getSchemaRegistry());
        props.put("specific.avro.reader", true);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        consumer = new KafkaConsumer<>(props);

        try {
            consumer.subscribe(Collections.singleton(topicProps.getHubs()));

            while (running) {

                ConsumerRecords<String, HubEventAvro> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, HubEventAvro> r : records) {

                    HubEventAvro event = r.value();
                    String hubId = event.getHubId().toString();

                    hubDevices.putIfAbsent(hubId, new HashSet<>());

                    Object payload = event.getPayload();

                    if (payload instanceof DeviceAddedEventAvro added) {
                        hubDevices.get(hubId).add(added.getId().toString());
                        log.info("Device added: {} to hub {}",
                                added.getId(), hubId);
                    }

                    if (payload instanceof DeviceRemovedEventAvro removed) {
                        hubDevices.get(hubId).remove(removed.getId().toString());
                        log.info("Device removed: {} from hub {}",
                                removed.getId(), hubId);
                    }
                }

                consumer.commitSync();
            }

        } catch (WakeupException e) {
            log.info("HubEventProcessor shutting down");
        } finally {
            consumer.close();
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (consumer != null) consumer.wakeup();
    }
}
