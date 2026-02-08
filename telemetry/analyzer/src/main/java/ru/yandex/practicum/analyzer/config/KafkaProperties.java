package ru.yandex.practicum.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties {

    private String bootstrapServers;
    private String schemaRegistryUrl;
    private ConsumerProperties consumer = new ConsumerProperties();

    @Getter
    @Setter
    public static class ConsumerProperties {
        private SnapshotConsumerProperties snapshotConsumer = new SnapshotConsumerProperties();
        private HubConsumerProperties hubConsumer = new HubConsumerProperties();
    }

    @Getter
    @Setter
    public static class SnapshotConsumerProperties {
        private String groupId = "analyzer-snapshot-group";
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = false;
        private int maxPollRecords = 100;
        private int maxPollIntervalMs = 300000;
        private int fetchMinSize = 1;
        private int fetchMaxWaitMs = 500;
    }

    @Getter
    @Setter
    public static class HubConsumerProperties {
        private String groupId = "analyzer-hub-group";
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = true;
        private int autoCommitIntervalMs = 5000;
        private int maxPollRecords = 50;
    }
}