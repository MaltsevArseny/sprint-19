package ru.yandex.practicum.aggregator.processor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.aggregator.config.KafkaProps;
import ru.yandex.practicum.aggregator.config.TopicProps;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;


import java.time.Duration;
import java.util.*;

@SuppressWarnings("unused")
@Component
@RequiredArgsConstructor
@Slf4j
public class AggregatorProcessor implements Runnable {

    private final KafkaProps kafkaProps;
    private final TopicProps topicProps;

    private final Map<CharSequence, SensorEventAvro> state = new HashMap<>();
    private volatile boolean running = true;

    @PostConstruct
    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {

        Properties cProps = new Properties();
        cProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProps.getBootstrapServers());
        cProps.put(ConsumerConfig.GROUP_ID_CONFIG, "aggregator");
        cProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        cProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        cProps.put("schema.registry.url", kafkaProps.getSchemaRegistry());
        cProps.put("specific.avro.reader", true);

        Properties pProps = new Properties();
        pProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProps.getBootstrapServers());
        pProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        pProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        pProps.put("schema.registry.url", kafkaProps.getSchemaRegistry());

        try (
                KafkaConsumer<String, SensorEventAvro> consumer = new KafkaConsumer<>(cProps);
                KafkaProducer<String, SensorsSnapshotAvro> producer = new KafkaProducer<>(pProps)
        ) {

            consumer.subscribe(Collections.singleton(topicProps.getSensors()));

            while (running) {

                ConsumerRecords<String, SensorEventAvro> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, SensorEventAvro> r : records) {

                    SensorEventAvro event = r.value();

                    state.put(event.getId(), event);

                    SensorsSnapshotAvro snapshot =
                            SensorsSnapshotAvro.newBuilder()
                                    .setHubId(event.getHubId())
                                    .setTimestamp(java.time.Instant.now())
                                    .setSensors(new ArrayList<>(state.values()))
                                    .build();

                    producer.send(new ProducerRecord<>(
                            topicProps.getSnapshots(),
                            event.getHubId().toString(),
                            snapshot
                    ));

                    log.info("Snapshot sent for hub {}", event.getHubId());
                }
            }
        }
    }

    public void shutdown() {
        running = false;
    }
}
