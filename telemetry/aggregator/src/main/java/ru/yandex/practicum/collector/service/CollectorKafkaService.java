package ru.yandex.practicum.collector.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.config.KafkaProps;
import ru.yandex.practicum.collector.config.TopicProps;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.util.Properties;

@SuppressWarnings("unused")
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectorKafkaService {

    private final KafkaProps kafkaProps;
    private final TopicProps topicProps;

    private KafkaProducer<String, Object> producer;

    private KafkaProducer<String, Object> getProducer() {

        if (producer == null) {

            Properties props = new Properties();

            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    kafkaProps.getBootstrapServers());

            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class);

            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    io.confluent.kafka.serializers.KafkaAvroSerializer.class);

            props.put("schema.registry.url",
                    kafkaProps.getSchemaRegistry());

            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.RETRIES_CONFIG, 3);

            producer = new KafkaProducer<>(props);
        }

        return producer;
    }

    // --- отправка sensor events ---
    public void sendSensorEvent(SensorEventAvro event) {

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(
                        topicProps.getSensors(),
                        event.getHubId().toString(),
                        event
                );

        getProducer().send(record, (meta, ex) -> {
            if (ex != null) {
                log.error("Failed to send sensor event", ex);
            } else {
                log.debug("Sensor event sent offset={}", meta.offset());
            }
        });
    }

    // --- отправка hub events ---
    public void sendHubEvent(HubEventAvro event) {

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(
                        topicProps.getHubs(),
                        event.getHubId().toString(),
                        event
                );

        getProducer().send(record, (meta, ex) -> {
            if (ex != null) {
                log.error("Failed to send hub event", ex);
            } else {
                log.debug("Hub event sent offset={}", meta.offset());
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        if (producer != null) {
            log.info("Closing Kafka producer...");
            producer.flush();
            producer.close();
        }
    }
}
