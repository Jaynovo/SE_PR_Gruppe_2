package at.jku.se.gruppe2.model;

import java.util.*;

public class Room {
    private int id;
    private String roomLabel;
    private double length;
    private double width;
    private double area;
    //private List<Sensor> sensors;
    //private List<Actuator> actuators;

    private List<Device> devices;
    /*Todo Uncomment when classes are created*/

    public Room() {}

    public  Room(int id, String roomLabel, double length, double width) {
        this.id = id;
        this.roomLabel = roomLabel;
        this.length = length;
        this.width = width;
        this.area = length * width;
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

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }
}
