package at.jku.se.gruppe2.model;

import java.util.*;

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

    public Double getArea() {
        return area;
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

    public <T extends Device> List<T> getDevicesOfType(Class<T> type) {
        return devices.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }
}
