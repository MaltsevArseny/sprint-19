package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.analyzer.entity.Sensor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface SensorRepository extends JpaRepository<Sensor, String> {
    List<Sensor> findByHubId(String hubId);
    Optional<Sensor> findByIdAndHubId(String id, String hubId);
    boolean existsByIdInAndHubId(Collection<String> ids, String hubId); // Добавить
    List<Sensor> findByTypeAndHubId(String type, String hubId);
}