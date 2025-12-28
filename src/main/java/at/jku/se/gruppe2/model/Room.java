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

    public  Room(int id, String roomLabel, double length, double width, int floor) {
        this.id = id;
        this.roomLabel = roomLabel;
        this.length = length;
        this.width = width;
        this.floor = floor;
        this.area = length * width;
    }

    public Room() {

    }

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

    public Double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
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
}
