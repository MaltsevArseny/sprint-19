package ru.yandex.practicum.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.analyzer.processor.HubEventProcessor;
import ru.yandex.practicum.analyzer.processor.SnapshotProcessor;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnalyzerApplication {

    public static void main(String[] args) {

        ConfigurableApplicationContext context =
                SpringApplication.run(AnalyzerApplication.class, args);

        HubEventProcessor hub = context.getBean(HubEventProcessor.class);
        SnapshotProcessor snap = context.getBean(SnapshotProcessor.class);

        Thread hubThread = new Thread(hub);
        Thread snapThread = new Thread(snap);

        hubThread.start();
        snapThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            hub.shutdown();
            snap.shutdown();
        }));
    }
}
