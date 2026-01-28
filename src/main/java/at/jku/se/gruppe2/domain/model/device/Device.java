package at.jku.se.gruppe2.domain.model.device;

public abstract class Device {
    private int id;
    private int roomId;
    private String label;
    private DeviceType type;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public DeviceCategory getCategory() {
        return type != null ? type.getCategory() : null;
    }

    public String getTypeLabel() {
        return type != null ? type.getLabel() : null;
    }

    public String getUnit() {
        return type != null ? type.getUnit() : null;
    }

    public enum DeviceCategory {SENSOR, ACTUATOR}
}