package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scenarios")
@Getter
@Setter
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", length = 255, unique = true)
    private String scenarioId;

    @Column(name = "hub_id", nullable = false, length = 255)
    private String hubId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id", insertable = false, updatable = false)
    private Hub hub;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Condition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Action> actions = new ArrayList<>();

    public Scenario() {
        this.active = true;
        this.priority = 0;
        this.conditions = new ArrayList<>();
        this.actions = new ArrayList<>();
    }

    @PrePersist
    protected void onCreate() {
        if (scenarioId == null) {
            scenarioId = "scenario_" + System.currentTimeMillis();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}