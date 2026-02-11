package at.jku.se.gruppe2.domain.model.home;

import at.jku.se.gruppe2.domain.model.device.Device;

import java.util.*;

/**
 * Represents a room within a {@link Home}
 * A room may contain multiple {@link Device} instances (sensors and actuators).
 * Optional geometric properties (length/width) can be used to derive an area.
 * Additionally, a room may define temperature limits (min/max) which can be used by automation logic
 */
public class Room {

    private int id;
    private String roomLabel;
    private Integer floor;
    private Double length;
    private Double width;
    private Double area;

    private Home home;
    private List<Device> devices = new ArrayList<>();

    private Double minTemperature;
    private Double maxTemperature;

    public Room() {}

    /**
     * Creates a room and computes area if both dimensions are provided.
     *
     * @param id        room identifier
     * @param roomLabel room label/name
     * @param length    room length in m
     * @param width     room width in m
     * @param floor     floor number
     */
    public Room(int id, String roomLabel, Double length, Double width, Integer floor) {
        this.id = id;
        this.roomLabel = roomLabel;
        this.length = length;
        this.width = width;
        this.floor = floor;
        // Only calculate area if both dimensions are provided
        this.area = (length != null && width != null) ? length * width : null;
    }

    /* Getter and Setter */

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoomLabel() {
        return roomLabel;
    }

    public void setRoomLabel(String roomLabel) {
        this.roomLabel = roomLabel;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public Home getHome() {
        return home;
    }

    public void setHome(Home home) {
        this.home = home;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    /* Temperature Settings */

    public Double getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(Double minTemperature) {
        this.minTemperature = minTemperature;
    }

    public Double getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(Double maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public boolean hasTemperatureLimits() {
        return minTemperature != null && maxTemperature != null;
    }

    /* Device List Helper */

    /**
     * Returns all devices in this room that are instances of the given type.
     *
     * @param type device subclass to filter for
     * @return list of devices of the requested type (never {@code null})
     * @param <T> device subtype
     */
    public <T extends Device> List<T> getDevicesOfType(Class<T> type) {
        return devices.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }
}
