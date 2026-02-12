package at.jku.se.gruppe2.domain.service.device;

import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;

import java.util.Optional;

/**
 * Service responsible for reading and writing actuator states.
 *
 * <p>This service abstracts access to actuator state persistence
 * via {@link DeviceRepository}. It provides simple methods to
 * retrieve the latest state or write a new one.</p>
 *
 * <p>Actuator states are stored as Strings. The interpretation of
 * the state format (e.g., "ON", "POS=50", "TRIGGERED") depends on
 * the specific actuator type.</p>
 */
public class ActuatorService {
    /**
     * Repository used for actuator state persistence.
     */
    private final DeviceRepository repo = new DeviceRepository();

    /**
     * Returns the latest actuator state for the given actuator ID.
     *
     * <p>If no state is stored, the provided {@code defaultState} is returned.</p>
     *
     * @param actuatorId   actuator device identifier
     * @param defaultState fallback state if no state exists
     * @return latest stored state or {@code defaultState} if none found
     */
    public String getStateOrDefault(int actuatorId, String defaultState) {
        Optional<String> s = repo.getLatestActuatorState(actuatorId);
        return s.orElse(defaultState);
    }

    /**
     * Persists a new actuator state.
     *
     * <p>This method delegates to the repository and does not perform
     * validation of the state format.</p>
     *
     * @param actuatorId actuator device identifier
     * @param state      new actuator state (format depends on actuator type)
     */
    public void setState(int actuatorId, String state) {
        repo.insertActuatorState(actuatorId, state);
    }
}
