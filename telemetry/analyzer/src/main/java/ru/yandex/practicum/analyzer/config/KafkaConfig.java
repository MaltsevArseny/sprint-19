package ru.yandex.practicum.analyzer.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SnapshotAvro;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, SnapshotAvro> snapshotConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                kafkaProperties.getConsumer().getSnapshotConsumer().getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                kafkaProperties.getSchemaRegistryUrl());
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                kafkaProperties.getConsumer().getSnapshotConsumer().getAutoOffsetReset());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                kafkaProperties.getConsumer().getSnapshotConsumer().isEnableAutoCommit());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                kafkaProperties.getConsumer().getSnapshotConsumer().getMaxPollRecords());
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                kafkaProperties.getConsumer().getSnapshotConsumer().getMaxPollIntervalMs());
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG,
                kafkaProperties.getConsumer().getSnapshotConsumer().getFetchMinSize());
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,
                kafkaProperties.getConsumer().getSnapshotConsumer().getFetchMaxWaitMs());
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, HubEventAvro> hubEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                kafkaProperties.getConsumer().getHubConsumer().getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                kafkaProperties.getSchemaRegistryUrl());
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                kafkaProperties.getConsumer().getHubConsumer().getAutoOffsetReset());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                kafkaProperties.getConsumer().getHubConsumer().isEnableAutoCommit());
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG,
                kafkaProperties.getConsumer().getHubConsumer().getAutoCommitIntervalMs());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                kafkaProperties.getConsumer().getHubConsumer().getMaxPollRecords());

        return new DefaultKafkaConsumerFactory<>(props);
    }
}