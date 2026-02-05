package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.KafkaProps;
import ru.yandex.practicum.analyzer.config.TopicProps;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class HubEventProcessor implements Runnable {

    private final KafkaProps kafkaProps;
    private final TopicProps topicProps;

    private final AtomicBoolean running = new AtomicBoolean(true);
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
        props.put("specific.avro.reader", true);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        consumer = new KafkaConsumer<>(props);

        consumer.subscribe(Collections.singleton(topicProps.getHubs()));

        while (running.get()) {

            ConsumerRecords<String, HubEventAvro> records =
                    consumer.poll(Duration.ofMillis(500));

            for (ConsumerRecord<String, HubEventAvro> r : records) {

                HubEventAvro event = r.value();

                log.info("Hub event received: hubId={}", event.getHubId());
            }

            consumer.commitSync();
        }
    }

    public void shutdown() {
        running.set(false);
        if (consumer != null) consumer.wakeup();
    }
}
