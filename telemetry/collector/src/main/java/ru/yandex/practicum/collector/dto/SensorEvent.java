package ru.yandex.practicum.collector.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public abstract class SensorEvent {

    private String id;
    private String hubId;
    private Instant timestamp;
    private SensorEventType type;

}
