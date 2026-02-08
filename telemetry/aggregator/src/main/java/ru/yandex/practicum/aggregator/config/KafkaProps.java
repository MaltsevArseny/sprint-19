package ru.yandex.practicum.aggregator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("unused")
@Data
@Configuration
@ConfigurationProperties(prefix = "kafka")
public class KafkaProps {

    private String bootstrapServers;
    private String schemaRegistry;
}
