package ru.yandex.practicum.collector.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.collector.service.CollectorKafkaService;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@SuppressWarnings("unused")
@GrpcService
@RequiredArgsConstructor
public class CollectorGrpcService extends CollectorGrpc.CollectorImplBase {

    private final CollectorKafkaService kafkaService;

    @Override
    public void collectSensorEvent(
            SensorEvent request,
            StreamObserver<CollectResponse> responseObserver) {

        SensorEventAvro avro = SensorEventAvro.newBuilder()
                .setId(request.getId())
                .setHubId(request.getHubId())
                .setTimestamp(request.getTimestamp())
                .build();

        kafkaService.sendSensorEvent(avro);

        responseObserver.onNext(
                CollectResponse.newBuilder()
                        .setSuccess(true)
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void collectHubEvent(
            HubEvent request,
            StreamObserver<CollectResponse> responseObserver) {

        HubEventAvro avro = HubEventAvro.newBuilder()
                .setHubId(request.getHubId())
                .setTimestamp(request.getTimestamp())
                .build();

        kafkaService.sendHubEvent(avro);

        responseObserver.onNext(
                CollectResponse.newBuilder()
                        .setSuccess(true)
                        .build()
        );
        responseObserver.onCompleted();
    }
}
