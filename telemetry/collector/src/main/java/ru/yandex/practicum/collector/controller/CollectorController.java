package ru.yandex.practicum.collector.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.collector.dto.SensorEvent;
import ru.yandex.practicum.collector.service.CollectorService;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CollectorController {

    private final CollectorService service;

    @PostMapping("/sensors")
    public void sensors(@RequestBody SensorEvent event) {
        service.collectSensor(event);
    }
}
