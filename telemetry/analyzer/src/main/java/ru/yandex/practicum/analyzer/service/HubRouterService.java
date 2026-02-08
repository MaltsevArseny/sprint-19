package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Empty;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

@SuppressWarnings("unused")
@Service
@Slf4j
public class HubRouterService {

    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub client;

    public HubRouterService(
            @GrpcClient("hub-router")
            HubRouterControllerGrpc.HubRouterControllerBlockingStub client) {
        this.client = client;
    }

    public void sendAction(
            String hubId,
            String scenarioName,
            String type,
            int value) {

        ru.yandex.practicum.grpc.telemetry.hubrouter.DeviceActionProto action =
                ru.yandex.practicum.grpc.telemetry.hubrouter.DeviceActionProto.newBuilder()
                        .setType(type)
                        .setValue(value)
                        .build();

        ru.yandex.practicum.grpc.telemetry.hubrouter.DeviceActionRequest request =
                ru.yandex.practicum.grpc.telemetry.hubrouter.DeviceActionRequest.newBuilder()
                        .setHubId(hubId)
                        .setScenarioName(scenarioName)
                        .setAction(action)
                        .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() / 1000)
                                .setNanos((int) ((System.currentTimeMillis() % 1000) * 1_000_000))
                                .build())
                        .build();

        try {
            Empty response = client.handleDeviceAction(request);
            log.info("Action sent: hub={}, scenario={}, type={}, value={}",
                    hubId, scenarioName, type, value);
        } catch (Exception e) {
            log.error("Failed to send action: {}", e.getMessage(), e);
        }
    }
}