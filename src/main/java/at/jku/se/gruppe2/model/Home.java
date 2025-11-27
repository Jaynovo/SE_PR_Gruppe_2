package at.jku.se.gruppe2.model;

public class Home {
    private String homeName;
    private int floors;
    private Address address;

    public Home(String homeName, int floors,  Address address) {
        this.homeName = homeName;
        this.floors = floors;
        this.address = address;
    }

    public String getHomeName() {
        return homeName;
    }
    public void setHomeName(String homeName) {
        this.homeName = homeName;
    }

    public int getFloors() {
        return floors;
    }

    public void setFloors(int floors) {
        this.floors = floors;
    }
    public Address getAddress() {
        return address;

    }
    public void setAddress(Address address) {
        this.address = address;
    }

}

