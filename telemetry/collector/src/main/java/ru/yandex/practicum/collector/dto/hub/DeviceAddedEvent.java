package ru.yandex.practicum.collector.dto.hub;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceAddedEvent extends HubEvent {

    private String id;
    private String type;
}
