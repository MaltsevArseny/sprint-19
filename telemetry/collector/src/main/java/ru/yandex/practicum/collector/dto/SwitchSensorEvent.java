package ru.yandex.practicum.collector.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SwitchSensorEvent extends SensorEvent {
    private boolean state;
}
