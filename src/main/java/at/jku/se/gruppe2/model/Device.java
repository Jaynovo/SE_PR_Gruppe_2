package at.jku.se.gruppe2.model;


/* TODO currently more or less a stub class and not abstract*/
public abstract class Device {
    private int id;
    private String label;
    private DeviceCategory category;
    private String type;
    private Room room;


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
