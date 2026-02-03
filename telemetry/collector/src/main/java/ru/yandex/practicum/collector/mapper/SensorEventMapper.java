package ru.yandex.practicum.collector.mapper;


import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.dto.*;
import ru.yandex.practicum.kafka.telemetry.event.*;


import java.time.Instant;

@Component
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEvent e) {

        SensorEventAvro avro = new SensorEventAvro();
        avro.setId(e.getId());
        avro.setHubId(e.getHubId());
        avro.setTimestamp(Instant.ofEpochSecond(e.getTimestamp().toEpochMilli()));

        switch (e.getType()) {

            case LIGHT_SENSOR_EVENT -> {
                LightSensorEvent l = (LightSensorEvent) e;
                avro.setPayload(new LightSensorAvro(
                        l.getLinkQuality(),
                        l.getLuminosity()));
            }

            case MOTION_SENSOR_EVENT -> {
                MotionSensorEvent m = (MotionSensorEvent) e;
                avro.setPayload(new MotionSensorAvro(
                        m.getLinkQuality(),
                        m.isMotion(),
                        m.getVoltage()));
            }

            case SWITCH_SENSOR_EVENT -> {
                SwitchSensorEvent s = (SwitchSensorEvent) e;
                avro.setPayload(new SwitchSensorAvro(s.isState()));
            }

            case CLIMATE_SENSOR_EVENT -> {
                ClimateSensorEvent c = (ClimateSensorEvent) e;
                avro.setPayload(new ClimateSensorAvro(
                        c.getTemperatureC(),
                        c.getHumidity(),
                        c.getCo2Level()));
            }

            case TEMPERATURE_SENSOR_EVENT -> {
                TemperatureSensorEvent t = (TemperatureSensorEvent) e;
                avro.setPayload(new TemperatureSensorAvro(
                        t.getTemperatureC(),
                        t.getTemperatureF()));

            }

        }

        return avro;
    }
}
