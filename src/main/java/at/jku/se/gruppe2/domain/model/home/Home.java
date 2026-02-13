package at.jku.se.gruppe2.domain.model.home;

import java.util.List;

/**
 * Represents a smart home consisting of an address and one or more rooms.
 * This model stores structural information such as number of floors and the list of rooms
 */
public class Home {
    private int id;
    private String homeLabel;
    private int floors;
    private Address address;
    private List<Room> rooms;

    public Home() {
    }

    /**
     * Creates a home with validation.
     *
     * @param homeLabel label/name of the home (must be at least 4 characters, not blank)
     * @param floors    number of floors (must be &gt; 0)
     * @param address   address of the home (must not be {@code null})
     * @throws IllegalArgumentException if any parameter violates the constraints
     */
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

    /**
     * @return rooms belonging to this home (may be {@code null} if not initialized)
     */
    public List<Room> getRooms() {
        return rooms;
    }

    /**
     * @param rooms list of rooms belonging to this home
     */
    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public Address getAddress() {
        return address;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

}

