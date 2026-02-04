package ru.yandex.practicum.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.analyzer.processor.HubEventProcessor;

@SpringBootApplication
public class AnalyzerApplication {

    public static void main(String[] args) {

        ConfigurableApplicationContext context =
                SpringApplication.run(AnalyzerApplication.class, args);

        HubEventProcessor processor =
                context.getBean(HubEventProcessor.class);

        // запускаем обработку в текущем потоке
        processor.run();
    }
}
