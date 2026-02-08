package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.analyzer.entity.Scenario;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    List<Scenario> findByHubId(String hubId);

    List<Scenario> findByHubIdAndActiveTrue(String hubId);

    @Query("SELECT s FROM Scenario s LEFT JOIN FETCH s.conditions LEFT JOIN FETCH s.actions WHERE s.hubId = :hubId AND s.active = true ORDER BY s.priority DESC")
    List<Scenario> findActiveScenariosWithDetails(@Param("hubId") String hubId);

    Optional<Scenario> findByHubIdAndName(String hubId, String name);

    Optional<Scenario> findByScenarioId(String scenarioId);

    boolean existsByHubIdAndName(String hubId, String name);

    @Query("SELECT COUNT(s) FROM Scenario s WHERE s.hubId = :hubId")
    long countByHubId(@Param("hubId") String hubId);
}