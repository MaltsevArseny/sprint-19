package ru.yandex.practicum.aggregator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("unused")
@Data
@Configuration
@ConfigurationProperties(prefix = "topics")
public class TopicProps {

    private String sensors;
    private String snapshots;
}
