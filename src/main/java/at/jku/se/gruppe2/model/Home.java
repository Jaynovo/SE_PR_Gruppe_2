package at.jku.se.gruppe2.model;

public class Home {
    private String homeLabel;
    private int floors;
    private Address address;

    public Home(String homeLabel, int floors, Address address) {
        this.homeLabel = homeLabel;
        this.floors = floors;
        this.address = address;
    }

    public String getHomeLabel() {
        return homeLabel;
    }
    public void setHomeLabel(String homeLabel) {
        this.homeLabel = homeLabel;
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

