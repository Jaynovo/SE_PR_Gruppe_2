package at.jku.se.gruppe2.model;

public class Home {
    private String homeLabel;
    private int floors;
    private Address address;

    public Home(String homeLabel, int floors, Address address) {
        if (homeLabel == null || homeLabel.isBlank() || homeLabel.length() < 4)
            throw new IllegalArgumentException("Home label must be at least 4 characters.");

        if (floors <= 0)
            throw new IllegalArgumentException("Home must have at least 1 floor.");

        if (address == null)
            throw new IllegalArgumentException("Address cannot be null.");

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

