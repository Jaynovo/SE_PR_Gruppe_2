package at.jku.se.gruppe2.domain.model.home;

/**
 * Represents a geographic location using latitude and longitude.
 * Coordinates are stored in decimal degrees
 */
public class Location {
    private double latitude;
    private double longitude;

    /**
     * Creates a new location.
     *
     * @param latitude  latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     */
    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    //Getter and Setter for now simple maybe needs change in later application
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
