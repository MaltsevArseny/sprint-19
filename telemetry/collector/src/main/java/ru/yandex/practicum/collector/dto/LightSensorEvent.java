package ru.yandex.practicum.collector.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LightSensorEvent extends SensorEvent {
    private int linkQuality;
    private int luminosity;
}
