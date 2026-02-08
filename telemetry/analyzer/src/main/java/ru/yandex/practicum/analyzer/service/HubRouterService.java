package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Empty;
import com.google.protobuf.util.Timestamps;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import ru.yandex.practicum.grpc.telemetry.hubrouter.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.hubrouter.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
@SuppressWarnings("unused")
@Service
@Slf4j
public class HubRouterService {

    private final HubRouterControllerGrpc
            .HubRouterControllerBlockingStub client;

    public HubRouterService(
            @GrpcClient("hub-router")
            HubRouterControllerGrpc
                    .HubRouterControllerBlockingStub client) {
        this.client = client;
    }

    public void sendAction(
            String hubId,
            String scenarioName,
            String type,
            int value){

        DeviceActionProto action =
                DeviceActionProto.newBuilder()
                        .setType(type)
                        .setValue(value)
                        .build();

        DeviceActionRequest request =
                DeviceActionRequest.newBuilder()
                        .setHubId(hubId)
                        .setScenarioName(scenarioName)
                        .setAction(action)
                        .setTimestamp(
                                Timestamps.fromMillis(
                                        System.currentTimeMillis()))
                        .build();

        Empty response =
                client.handleDeviceAction(request);

        log.info("Action sent: hub={}, scenario={}",
                hubId, scenarioName);
    }
}
