package ru.yandex.practicum.collector.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MotionSensorEvent extends SensorEvent {
    private int linkQuality;
    private boolean motion;
    private int voltage;
}
