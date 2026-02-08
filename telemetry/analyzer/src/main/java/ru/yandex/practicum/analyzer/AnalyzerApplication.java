package ru.yandex.practicum.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import ru.yandex.practicum.analyzer.processor.HubEventProcessor;
import ru.yandex.practicum.analyzer.processor.SnapshotProcessor;

@Slf4j
@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
public class AnalyzerApplication {

    public static void main(String[] args) {
        log.info("Starting Analyzer application...");

        ConfigurableApplicationContext ctx =
                SpringApplication.run(AnalyzerApplication.class, args);

        // Получаем бины процессоров
        HubEventProcessor hubProcessor = ctx.getBean(HubEventProcessor.class);
        SnapshotProcessor snapshotProcessor = ctx.getBean(SnapshotProcessor.class);

        // Запускаем обработку событий хаба в отдельном потоке
        Thread hubThread = new Thread(hubProcessor, "hub-event-processor");
        hubThread.setDaemon(true);
        hubThread.start();
        log.info("Hub event processor started in thread: {}", hubThread.getName());

        // Запускаем обработку снапшотов в отдельном потоке
        Thread snapshotThread = new Thread(snapshotProcessor, "snapshot-processor");
        snapshotThread.setDaemon(true);
        snapshotThread.start();
        log.info("Snapshot processor started in thread: {}", snapshotThread.getName());

        // Добавляем обработчик корректного завершения
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received, stopping processors...");

            hubProcessor.shutdown();
            snapshotProcessor.shutdown();

            try {
                hubThread.join(5000);
                snapshotThread.join(5000);
                log.info("Processors stopped successfully");
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for processors to stop");
                Thread.currentThread().interrupt();
            }

            ctx.close();
            log.info("Analyzer application shutdown complete");
        }));

        log.info("Analyzer application started successfully");

        // Ждем завершения потоков (блокируем основной поток)
        try {
            hubThread.join();
            snapshotThread.join();
        } catch (InterruptedException e) {
            log.error("Main thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}