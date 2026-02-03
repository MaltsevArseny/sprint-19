package ru.yandex.practicum.collector.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.collector.dto.SensorEvent;
import ru.yandex.practicum.collector.dto.hub.HubEvent;
import ru.yandex.practicum.collector.service.CollectorService;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class CollectorController {

    private final CollectorService collectorService;


    // ✅ HUB
    @PostMapping("/hubs")
    public void collectHub(@RequestBody HubEvent event) {
        collectorService.collectHubEvent(event);
    }

    // ✅ SENSOR
    @PostMapping("/sensors")
    public void collectSensor(@RequestBody SensorEvent event) {
        collectorService.collectSensorEvent(event);
    }
}


