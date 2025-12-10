package at.jku.se.gruppe2.model;


public abstract class Device {
    private int id;
    private String label;
    private DeviceCategory category;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public enum DeviceCategory {
        SENSOR,
        ACTUATOR
    }
}
