package at.jku.se.gruppe2.domain.service.device;

import at.jku.se.gruppe2.domain.model.device.config.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing actuator configurations and small per-actuator runtime counters.
 *
 * <p>This service stores configurations in-memory keyed by actuator device ID.
 * It also maintains simple counters used by automations (e.g., alarm debounce counter,
 * cat feeder cooldown/feeding ticks).</p>
 *
 * <p><b>Thread-safety:</b> Most configuration maps use {@link ConcurrentHashMap} to allow
 * concurrent reads/writes. Note that {@code catFeederFeedingTicks} currently uses a {@code HashMap}
 * and is therefore not thread-safe if accessed concurrently.</p>
 *
 * <p><b>Permission checks:</b> Permission/authorization checks are intentionally not handled here
 * and should be performed in the calling layer (e.g., controllers/services that enforce user roles).</p>
 */
public class ActuatorConfigService {

    /**
     * Ventilation configuration per actuator device ID.
     */
    private final Map<Integer, VentilationConfig> ventilationConfigByActuatorId = new ConcurrentHashMap<>();

    /**
     * Manual on/off state per actuator device ID (used if auto mode is disabled).
     */
    private final Map<Integer, Boolean> manualOnByActuatorId = new ConcurrentHashMap<>();

    /**
     * Returns the ventilation configuration for the given actuator ID, creating a default config if absent.
     *
     * @param actuatorDeviceId actuator device identifier
     * @return existing or newly created {@link VentilationConfig}
     */
    public VentilationConfig getOrCreateVentilationConfig(int actuatorDeviceId) {
        return ventilationConfigByActuatorId.computeIfAbsent(actuatorDeviceId, id -> new VentilationConfig());
    }

    /**
     * Stores a ventilation configuration for the given actuator ID.
     *
     * @param actuatorDeviceId actuator device identifier
     * @param cfg              configuration to store
     */
    public void saveVentilationConfig(int actuatorDeviceId, VentilationConfig cfg) {
        ventilationConfigByActuatorId.put(actuatorDeviceId, cfg);
    }

    public boolean isManualOn(int actuatorDeviceId) {
        return manualOnByActuatorId.getOrDefault(actuatorDeviceId, false);
    }

    public void setManualOn(int actuatorDeviceId, boolean on) {
        manualOnByActuatorId.put(actuatorDeviceId, on);
    }

    // ALARMSYSTEM
    private final Map<Integer, AlarmConfig> alarmConfigByActuatorId = new ConcurrentHashMap<>();

    /**
     * Debounce counter: number of consecutive "too loud" ticks per alarm actuator device ID.
     */
    private final Map<Integer, Integer> alarmNoiseCounterByActuatorId = new ConcurrentHashMap<>();

    /**
     * Returns the alarm configuration for the given actuator ID, creating a default config if absent.
     *
     * @param actuatorDeviceId actuator device identifier
     * @return existing or newly created {@link AlarmConfig}
     */
    public AlarmConfig getOrCreateAlarmConfig(int actuatorDeviceId) {
        return alarmConfigByActuatorId.computeIfAbsent(actuatorDeviceId, id -> new AlarmConfig());
    }

    /**
     * Stores an alarm configuration for the given actuator ID.
     *
     * @param actuatorDeviceId actuator device identifier
     * @param cfg              configuration to store
     */
    public void saveAlarmConfig(int actuatorDeviceId, AlarmConfig cfg) {
        alarmConfigByActuatorId.put(actuatorDeviceId, cfg);
    }

    /**
     * Returns the current alarm noise debounce counter for the given actuator ID.
     *
     * @param actuatorDeviceId actuator device identifier
     * @return debounce counter (defaults to 0 if not present)
     */
    public int getAlarmNoiseCounter(int actuatorDeviceId) {
        return alarmNoiseCounterByActuatorId.getOrDefault(actuatorDeviceId, 0);
    }

    /**
     * Sets the alarm noise debounce counter for the given actuator ID.
     *
     * @param actuatorDeviceId actuator device identifier
     * @param value            new counter value
     */
    public void setAlarmNoiseCounter(int actuatorDeviceId, int value) {
        alarmNoiseCounterByActuatorId.put(actuatorDeviceId, value);
    }

    /**
     * Resets the alarm noise debounce counter to 0 for the given actuator ID.
     *
     * @param actuatorDeviceId actuator device identifier
     */
    public void resetAlarmNoiseCounter(int actuatorDeviceId) {
        alarmNoiseCounterByActuatorId.put(actuatorDeviceId, 0);
    }
    //CAT FEEDER
    /**
     * Cat feeder configuration per actuator device ID.
     */
    private final Map<Integer, CatFeederConfig> catFeederCfg = new ConcurrentHashMap<>();
    /**
     * Cooldown ticks per cat feeder actuator ID.
     */
    private final Map<Integer, Integer> catFeederCooldown = new ConcurrentHashMap<>();
    /**
     * Feeding ticks per cat feeder actuator ID.
     */
    private final java.util.Map<Integer, Integer> catFeederFeedingTicks = new java.util.HashMap<>();

    /**
     * Returns the remaining feeding ticks for the given cat feeder actuator.
     *
     * @param actuatorId actuator device identifier
     * @return feeding ticks remaining (defaults to 0)
     */
    public int getCatFeederFeedingTicks(int actuatorId) {
        return catFeederFeedingTicks.getOrDefault(actuatorId, 0);
    }

    /**
     * Sets feeding ticks for the given cat feeder actuator.
     * Values below 0 are clamped to 0.
     *
     * @param actuatorId actuator device identifier
     * @param ticks      feeding ticks (non-negative)
     */
    public void setCatFeederFeedingTicks(int actuatorId, int ticks) {
        catFeederFeedingTicks.put(actuatorId, Math.max(0, ticks));
    }

    public int decrementCatFeederFeedingTicks(int actuatorId) {
        int current = getCatFeederFeedingTicks(actuatorId);
        if (current <= 0) return 0;
        int next = current - 1;
        setCatFeederFeedingTicks(actuatorId, next);
        return next;
    }

    /**
     * Returns the cat feeder configuration for the given actuator ID, creating a default config if absent.
     *
     * @param actuatorId actuator device identifier
     * @return existing or newly created {@link CatFeederConfig}
     */
    public CatFeederConfig getOrCreateCatFeederConfig(int actuatorId) {
        return catFeederCfg.computeIfAbsent(actuatorId, id -> new CatFeederConfig());
    }

    /**
     * Stores a cat feeder configuration for the given actuator ID.
     *
     * @param actuatorId actuator device identifier
     * @param cfg        configuration to store
     */
    public void saveCatFeederConfig(int actuatorId, CatFeederConfig cfg) {
        catFeederCfg.put(actuatorId, cfg);
    }


    /**
     * Returns the cooldown ticks remaining for the given cat feeder actuator.
     *
     * @param actuatorId actuator device identifier
     * @return cooldown ticks remaining (defaults to 0)
     */
    public int getCatFeederCooldown(int actuatorId) {
        return catFeederCooldown.getOrDefault(actuatorId, 0);
    }

    /**
     * Sets the cooldown ticks for the given cat feeder actuator.
     * Values below 0 are clamped to 0.
     *
     * @param actuatorId actuator device identifier
     * @param ticks      cooldown ticks (non-negative)
     */
    public void setCatFeederCooldown(int actuatorId, int ticks) {
        catFeederCooldown.put(actuatorId, Math.max(0, ticks));
    }

    public int decrementCatFeederCooldown(int actuatorId) {
        int current = getCatFeederCooldown(actuatorId);
        if (current <= 0) return 0;
        int next = current - 1;
        setCatFeederCooldown(actuatorId, next);
        return next;
    }

    // HEATING
    /**
     * Heating configuration per actuator device ID.
     */
    private final Map<Integer, HeatingConfig> heatingCfgByActuatorId = new ConcurrentHashMap<>();

    /**
     * Returns the heating configuration for the given actuator ID, creating a default config if absent.
     *
     * @param actuatorId actuator device identifier
     * @return existing or newly created {@link HeatingConfig}
     */
    public HeatingConfig getOrCreateHeatingConfig(int actuatorId) {
        return heatingCfgByActuatorId.computeIfAbsent(actuatorId, id -> new HeatingConfig());
    }

    /**
     * Stores a heating configuration for the given actuator ID.
     *
     * @param actuatorId actuator device identifier
     * @param cfg        configuration to store
     */
    public void saveHeatingConfig(int actuatorId, HeatingConfig cfg) {
        heatingCfgByActuatorId.put(actuatorId, cfg);
    }

    // BLINDS
    /**
     * Blinds configuration per actuator device ID.
     */

    private final Map<Integer, BlindsConfig> blindsCfgByActuatorId = new ConcurrentHashMap<>();

    /**
     * Returns the blinds configuration for the given actuator ID, creating a default config if absent.
     *
     * @param actuatorId actuator device identifier
     * @return existing or newly created {@link BlindsConfig}
     */
    public BlindsConfig getOrCreateBlindsConfig(int actuatorId) {
        return blindsCfgByActuatorId.computeIfAbsent(actuatorId, id -> new BlindsConfig());
    }

    /**
     * Stores a blinds configuration for the given actuator ID.
     *
     * @param actuatorId actuator device identifier
     * @param cfg        configuration to store
     */
    public void saveBlindsConfig(int actuatorId, BlindsConfig cfg) {
        blindsCfgByActuatorId.put(actuatorId, cfg);
    }

    public void resetCatFeederCooldown(int actuatorId) {
        catFeederCooldown.remove(actuatorId);
    }
}