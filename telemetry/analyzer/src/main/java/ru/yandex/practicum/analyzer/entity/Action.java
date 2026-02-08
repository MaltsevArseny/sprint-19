package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "actions")
@Getter
@Setter
public class Action {

    public enum ActionType {
        TURN_ON, TURN_OFF, SET_VALUE, ADJUST, TOGGLE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_id", length = 255)
    private String actionId;

    @Column(name = "scenario_id", nullable = false, length = 255)
    private String scenarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", insertable = false, updatable = false)
    private Scenario scenario;

    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType type;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "value")
    private Integer value;

    @Column(name = "delay_ms")
    private Integer delayMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (actionId == null) {
            actionId = "action_" + System.currentTimeMillis();
        }
        if (delayMs == null) {
            delayMs = 0;
        }
        createdAt = LocalDateTime.now();
    }
}