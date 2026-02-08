package ru.yandex.practicum.collector.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemperatureSensorEvent extends SensorEvent {
    private int temperatureC;
    private int temperatureF;
}
