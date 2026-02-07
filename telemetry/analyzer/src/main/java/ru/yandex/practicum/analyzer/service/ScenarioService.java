package ru.yandex.practicum.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@SuppressWarnings("unused")
@Service
@Slf4j
public class ScenarioService {

    public void process(String hubId) {
        log.info("Processing scenarios for hub {}", hubId);
    }
}
