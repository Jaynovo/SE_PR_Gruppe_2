package at.jku.se.gruppe2.model;

public class DeviceType {
    private int id;
    private Device.DeviceCategory category; // SENSOR / ACTUATOR
    private String label;                  // z.B. "CO2Sensor", "Ventilation"
    private String unit;                   // z.B. "ppm", "dB" (bei Aktoren meist null)

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Device.DeviceCategory getCategory() {
        return category;
    }

    public void setCategory(Device.DeviceCategory category) {
        this.category = category;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    //für ComboBox zum anzeigen
    @Override
    public String toString() {
        if (unit == null || unit.isBlank()) return label;
        return label + " (" + unit + ")";
    }
}
