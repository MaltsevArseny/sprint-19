package ru.yandex.practicum.analyzer.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnalyzerHealthIndicator {

    public boolean isHealthy() {
        // Простая проверка здоровья
        try {
            // Здесь можно добавить реальные проверки
            return true;
        } catch (Exception e) {
            log.error("Health check failed", e);
            return false;
        }
    }
}