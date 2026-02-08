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

        ConfigurableApplicationContext ctx =
                SpringApplication.run(AnalyzerApplication.class, args);

        HubEventProcessor hub = ctx.getBean(HubEventProcessor.class);
        SnapshotProcessor snap = ctx.getBean(SnapshotProcessor.class);

        Thread hubThread = new Thread(hub);
        hubThread.setName("hub-events");
        hubThread.start();

        Thread snapThread = new Thread(snap);
        snapThread.setName("snapshots");
        snapThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            hub.shutdown();
            snap.shutdown();
        }));
    }
}
