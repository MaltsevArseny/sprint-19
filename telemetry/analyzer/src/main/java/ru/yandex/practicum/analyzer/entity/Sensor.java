package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "sensors")
@Getter
@Setter
public class Sensor {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "hub_id", nullable = false, length = 255)
    private String hubId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id", insertable = false, updatable = false)
    private Hub hub;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "last_value")
    private Double lastValue;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Version
    @Column(name = "version")
    private Long version;

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
}