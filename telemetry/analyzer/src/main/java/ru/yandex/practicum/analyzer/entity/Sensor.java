package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="sensors")
@Getter @Setter
public class Sensor {

    @Id
    private String id;

    private String hubId;
}
