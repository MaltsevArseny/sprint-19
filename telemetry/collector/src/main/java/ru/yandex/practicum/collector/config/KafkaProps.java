package ru.yandex.practicum.collector.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("unused")
@Configuration
@ConfigurationProperties(prefix = "kafka")
@Getter
@Setter
public class KafkaProps {

    private String bootstrapServers;
    private String schemaRegistry;
}
