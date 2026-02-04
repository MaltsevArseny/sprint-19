package ru.yandex.practicum.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "analyzer.topics")
@Getter
@Setter
public class TopicProps {
    private String hubs;
    private String snapshots;
}
