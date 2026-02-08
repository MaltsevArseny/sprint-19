package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.analyzer.entity.Sensor;

@SuppressWarnings("unused")
public interface SensorRepository
        extends JpaRepository<Sensor,String> {}
