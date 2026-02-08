package ru.yandex.practicum.analyzer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("unused")
@Configuration
@ConfigurationProperties(prefix="kafka")
@Data
public class KafkaProps {

    private String bootstrapServers;
    private String schemaRegistry;
    private String snapshotTopic;
    private String hubTopic;
    private String snapshotGroup;
    private String hubGroup;
}
