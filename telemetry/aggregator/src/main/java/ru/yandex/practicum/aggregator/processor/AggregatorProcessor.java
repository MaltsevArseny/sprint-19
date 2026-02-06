package ru.yandex.practicum.aggregator.processor;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SnapshotAvro;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@SuppressWarnings("unused")
public class AggregatorProcessor implements Runnable {

    private static final Logger log =
            LoggerFactory.getLogger(AggregatorProcessor.class);

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final KafkaProducer<String, SnapshotAvro> producer;

    private volatile boolean running = true;

    public AggregatorProcessor(Properties consumerProps,
                               Properties producerProps) {

        this.consumer = new KafkaConsumer<>(consumerProps);
        this.producer = new KafkaProducer<>(producerProps);

        consumer.subscribe(Collections.singletonList("hub-events"));
    }

    @Override
    public void run() {

        try {
            while (running) {

                ConsumerRecords<String, HubEventAvro> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, HubEventAvro> record : records) {

                    SnapshotAvro snapshot =
                            aggregate(record.value());

                    producer.send(new ProducerRecord<>(
                            "snapshots",
                            record.key(),
                            snapshot
                    ));
                }

                consumer.commitSync();
            }

        } catch (WakeupException e) {
            log.info("Shutdown signal received");
        } catch (Exception e) {
            log.error("Error in aggregator", e);
        } finally {

            try {
                producer.flush();
                consumer.commitSync();
            } finally {
                consumer.close();
                producer.close();
            }
        }
    }

    private SnapshotAvro aggregate(HubEventAvro event) {

        return SnapshotAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(System.currentTimeMillis())
                .build();
    }

    public void shutdown() {
        running = false;
        consumer.wakeup();
    }
}
