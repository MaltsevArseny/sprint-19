package ru.yandex.practicum.analyzer.processor;

import org.apache.kafka.clients.consumer.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class HubEventProcessor implements Runnable {

    private final AtomicBoolean running = new AtomicBoolean(true);

    @Override
    public void run() {

        try (KafkaConsumer<String, byte[]> consumer =
                     new KafkaConsumer<>(consumerProps())) {

            consumer.subscribe(Collections.singleton("telemetry.hubs.v1"));

            while (running.get()) {
                ConsumerRecords<String, byte[]> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, byte[]> r : records) {
                    System.out.println("Hub event: " + r.key());
                }
            }
        }
    }

    private Properties consumerProps() {
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-hubs");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }
}
