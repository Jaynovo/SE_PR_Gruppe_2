package at.jku.se.gruppe2.domain.model.device;

/**
 * Base class for all devices in the smart home
 * A device can be either a sensor or an actuator, defined by its {@link DeviceType}
 * Devices are assigned to a room via {@link #roomId}
 */
public abstract class Device {
    private int id;
    private int roomId;
    private String label;
    private DeviceType type;

    /**
     * @return unique device identifier
     */
    public int getId() {
        return id;
    }

    /**
     * @param id unique device identifier
     */
    public void setId(int id) {
        this.id = id;
    }
    /**
     * @return identifier of the room this device belongs to
     */
    public int getRoomId() {
        return roomId;
    }
    /**
     * @param roomId room identifier the device belongs to
     */
    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }
    /**
     * @return name of the device label that the user typed in  (e.g., "Living Room CO2")
     */
    public String getLabel() {
        return label;
    }
    /**
     * @param label name of the device label that the user typed in
     */
    public void setLabel(String label) {
        this.label = label;
    }
    /**
     * @return device type metadata or {@code null} if not assigned
     */
    public DeviceType getType() {
        return type;
    }
    /**
     * @param type device type metadata (can be {@code null})
     */
    public void setType(DeviceType type) {
        this.type = type;
    }
    /**
     * Convenience accessor for the device category (sensor/actuator).
     * @return device category or {@code null} if {@link #type} is not set
     */
    public DeviceCategory getCategory() {
        return type != null ? type.getCategory() : null;
    }
    /**
     * Convenience accessor for a human-readable type label.
     * @return type label or {@code null} if {@link #type} is not set
     */
    public String getTypeLabel() {
        return type != null ? type.getLabel() : null;
    }
    /**
     * Convenience accessor for the display unit of this device (if applicable).
     *
     * @return unit string (e.g., "ppm", "°C") or {@code null} if not defined
     */
    public String getUnit() {
        return type != null ? type.getUnit() : null;
    }
    /**
     * Device category used to distinguish sensors from actuators.
     */
    public enum DeviceCategory {SENSOR, ACTUATOR}
}