package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.entity.Scenario;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;

import java.util.List;

@SuppressWarnings("unused")
@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository repository;

    public List<Scenario> findByHub(String hubId) {
        return repository.findByHubId(hubId);
    }
}
