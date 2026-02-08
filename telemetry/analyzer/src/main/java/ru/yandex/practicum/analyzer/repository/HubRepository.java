package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.analyzer.entity.Hub;

import java.util.Optional;

@Repository
public interface HubRepository extends JpaRepository<Hub, String> {

    @Query("SELECT h FROM Hub h LEFT JOIN FETCH h.sensors LEFT JOIN FETCH h.scenarios WHERE h.id = :id")
    Optional<Hub> findByIdWithDetails(@Param("id") String id);

    boolean existsById(String id);

    void deleteById(String id);
}