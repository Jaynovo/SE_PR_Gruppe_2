package at.jku.se.gruppe2.domain.service.device;

import at.jku.se.gruppe2.domain.model.device.actuator.SmartPlugActuator;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
/**
 * Handles power state and energy consumption tracking
 */
public class SmartPlugService {

    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    public SmartPlugService() {
        this.deviceRepository = new DeviceRepository();
        this.objectMapper = new ObjectMapper();
    }

    public SmartPlugService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Toggle the power state of a smart plug
     */
    public void togglePower(SmartPlugActuator plug) {
        plug.togglePower();
        savePlugState(plug);
    }

    /**
     * Set the power state explicitly
     */
    public void setPowerState(SmartPlugActuator plug, boolean powerOn) {
        plug.setPowerState(powerOn);
        savePlugState(plug);
    }

    /**
     * Load the current state from the database
     */
    public void loadPlugState(SmartPlugActuator plug) {
        Optional<String> stateJson = deviceRepository.getLatestActuatorState(plug.getId());

        if (stateJson.isPresent()) {
            try {
                Map<String, Object> state = objectMapper.readValue(stateJson.get(), Map.class);

                plug.setPowerState((Boolean) state.getOrDefault("powerState", false));

                if (state.containsKey("currentPowerUsage")) {
                    plug.setCurrentPowerUsage(((Number) state.get("currentPowerUsage")).doubleValue());
                }

                if (state.containsKey("totalEnergyUsed")) {
                    plug.setTotalEnergyUsed(((Number) state.get("totalEnergyUsed")).doubleValue());
                }

            } catch (JsonProcessingException e) {
                System.err.println("Error parsing smart plug state: " + e.getMessage());
            }
        }
    }

    /**
     * Save the current state to the database
     */
    public void savePlugState(SmartPlugActuator plug) {
        try {
            Map<String, Object> state = new HashMap<>();
            state.put("powerState", plug.isPowerOn());
            state.put("currentPowerUsage", plug.getCurrentPowerUsage());
            state.put("totalEnergyUsed", plug.getTotalEnergyUsed());

            String stateJson = objectMapper.writeValueAsString(state);
            deviceRepository.insertActuatorState(plug.getId(), stateJson);

        } catch (JsonProcessingException e) {
            System.err.println("Error saving smart plug state: " + e.getMessage());
        }
    }

    /**
     * Update power consumption metrics
     */
    public void updatePowerMetrics(SmartPlugActuator plug, double currentPowerUsage) {
        plug.setCurrentPowerUsage(currentPowerUsage);

        // Calculate energy consumed since last update (simplified)
        // In a real implementation, you'd track time delta
        double energyDelta = currentPowerUsage / 1000.0; // Convert W to kWh (simplified)
        plug.setTotalEnergyUsed(plug.getTotalEnergyUsed() + energyDelta);

        savePlugState(plug);
    }

    /**
     * Get a human-readable status string
     */
    public String getStatusString(SmartPlugActuator plug) {
        StringBuilder status = new StringBuilder();
        status.append(plug.isPowerOn() ? "ON" : "OFF");

        if (plug.isPowerOn() && plug.getCurrentPowerUsage() != null && plug.getCurrentPowerUsage() > 0) {
            status.append(" (").append(String.format("%.1f W", plug.getCurrentPowerUsage())).append(")");
        }

        return status.toString();
    }

    /**
     * Calculate estimated cost based on energy usage
     * @param plug The smart plug
     * @param costPerKwh Cost per kilowatt-hour in your currency
     * @return Estimated total cost
     */
    public double calculateEnergyCost(SmartPlugActuator plug, double costPerKwh) {
        if (plug.getTotalEnergyUsed() == null) {
            return 0.0;
        }
        return plug.getTotalEnergyUsed() * costPerKwh;
    }
}