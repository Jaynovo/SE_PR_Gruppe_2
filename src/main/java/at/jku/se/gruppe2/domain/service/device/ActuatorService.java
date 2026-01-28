package at.jku.se.gruppe2.domain.service.device;

import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;

import java.util.Optional;

public class ActuatorService {
    private final DeviceRepository repo = new DeviceRepository();

    public String getStateOrDefault(int actuatorId, String defaultState) {
        Optional<String> s = repo.getLatestActuatorState(actuatorId);
        return s.orElse(defaultState);
    }

    public void setState(int actuatorId, String state) {
        repo.insertActuatorState(actuatorId, state);
    }
}
