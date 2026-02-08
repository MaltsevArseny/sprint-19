package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "conditions")
@Getter
@Setter
public class Condition {

    public enum Operator {
        GREATER, LESS, EQUAL, NOT_EQUAL, BETWEEN
    }

    public enum LogicalOperator {
        AND, OR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "condition_id", length = 255)
    private String conditionId;

    @Column(name = "scenario_id", nullable = false, length = 255)
    private String scenarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", insertable = false, updatable = false)
    private Scenario scenario;

    @Column(name = "sensor_id", length = 255)
    private String sensorId;

    @Column(name = "sensor_type", nullable = false, length = 50)
    private String sensorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 20)
    private Operator operation;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @Column(name = "threshold_value2")
    private Double thresholdValue2;

    @Enumerated(EnumType.STRING)
    @Column(name = "logical_operator", length = 10)
    private LogicalOperator logicalOperator = LogicalOperator.AND;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (conditionId == null) {
            conditionId = "condition_" + System.currentTimeMillis();
        }
        createdAt = LocalDateTime.now();
    }

    public boolean isMet(Double currentValue) {
        if (currentValue == null) return false;

        return switch (operation) {
            case GREATER -> currentValue > thresholdValue;
            case LESS -> currentValue < thresholdValue;
            case EQUAL -> Math.abs(currentValue - thresholdValue) < 0.001;
            case NOT_EQUAL -> Math.abs(currentValue - thresholdValue) > 0.001;
            case BETWEEN -> thresholdValue2 != null &&
                    currentValue >= thresholdValue &&
                    currentValue <= thresholdValue2;
        };
    }
}