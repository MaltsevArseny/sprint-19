package ru.yandex.practicum.collector.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.collector.service.CollectorKafkaService;
import ru.yandex.practicum.grpc.telemetry.event.*;

@SuppressWarnings("unused")
@GrpcService
@RequiredArgsConstructor
public class CollectorGrpcService extends CollectorGrpc.CollectorImplBase {

    private final CollectorKafkaService kafkaService;

    @Override
    public void collectSensorEvent(
            SensorEvent request,
            StreamObserver<CollectResponse> responseObserver) {

        kafkaService.sendSensor(request);

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

        kafkaService.sendHub(request);

        responseObserver.onNext(
                CollectResponse.newBuilder()
                        .setSuccess(true)
                        .build()
        );
        responseObserver.onCompleted();
    }
}
