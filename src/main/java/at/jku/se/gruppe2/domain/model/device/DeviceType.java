package at.jku.se.gruppe2.domain.model.device;

/**
 *  Metadata describing a device type
 *  Used for categorization (sensor/actuator), UI labels and display units
 */

public class DeviceType {
    private int id;
    private Device.DeviceCategory category; // SENSOR / ACTUATOR
    private String label;                  // z.B. "CO2Sensor", "Ventilation"
    private String unit;                   // z.B. "ppm", "dB"

    /**
     * @return unique device type identifier
     */
    public int getId() {
        return id;
    }
    /**
     * @param id unique device type identifier
     */
    public void setId(int id) {
        this.id = id;
    }
    /**
     * @return device category (sensor or actuator)
     */
    public Device.DeviceCategory getCategory() {
        return category;
    }
    /**
     * @param category device category (sensor or actuator)
     */
    public void setCategory(Device.DeviceCategory category) {
        this.category = category;
    }
    /**
     * @return human-readable type label (e.g., "CO2Sensor", "Ventilation")
     */
    public String getLabel() {
        return label;
    }
    /**
     * @param label human-readable type label
     */
    public void setLabel(String label) {
        this.label = label;
    }
    /**
     * @return display unit (e.g., "ppm", "dB") or {@code null} if not applicable
     */
    public String getUnit() {
        return unit;
    }
    /**
     * @param unit display unit (may be {@code null})
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }
    /**
     * Returns a user-friendly representation for UI components such as ComboBoxes.
     *
     * @return label or "label (unit)" if unit is available
     */
    @Override
    public String toString() {
        if (unit == null || unit.isBlank()) return label;
        return label + " (" + unit + ")";
    }
}
