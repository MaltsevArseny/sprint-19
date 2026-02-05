package ru.yandex.practicum.aggregator.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class TopicProps {

    @Value("${topics.sensors:sensors}")
    private String sensors;

    @Value("${topics.snapshots:snapshots}")
    private String snapshots;
}
