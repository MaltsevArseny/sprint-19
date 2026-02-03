package ru.yandex.practicum.collector.dto.hub;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public abstract class HubEvent {

    private String hubId;
    private Instant timestamp = Instant.now();
}
