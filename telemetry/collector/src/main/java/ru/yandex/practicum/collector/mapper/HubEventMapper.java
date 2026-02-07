package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.dto.hub.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
public class HubEventMapper {

    public HubEventAvro toAvro(HubEvent e) {

        HubEventAvro avro = new HubEventAvro();

        avro.setHubId(e.getHubId());
        avro.setTimestamp(e.getTimestamp());

        if (e instanceof DeviceAddedEvent a) {

            avro.setPayload(
                    new DeviceAddedEventAvro(
                            a.getId(),
                            DeviceTypeAvro.valueOf(a.getType())
                    )
            );

        } else if (e instanceof DeviceRemovedEvent r) {

            avro.setPayload(
                    new DeviceRemovedEventAvro(r.getId())
            );

        }

        return avro;
    }
}
