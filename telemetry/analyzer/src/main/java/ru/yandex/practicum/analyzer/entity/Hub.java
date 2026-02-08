package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hubs")
@Getter
@Setter
public class Hub {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "hub", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sensor> sensors = new ArrayList<>();

    @OneToMany(mappedBy = "hub", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Scenario> scenarios = new ArrayList<>();

    public Hub() {
        this.sensors = new ArrayList<>();
        this.scenarios = new ArrayList<>();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}