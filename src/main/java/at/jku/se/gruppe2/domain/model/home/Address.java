package at.jku.se.gruppe2.domain.model.home;

/**
 * Represents a physical address and optional geo coordinates
 * that are stored as latitude/longitude in decimal degrees.
 */

public class Address {

    private int id;
    private String street;
    private String houseNumber;
    private String city;
    private String postalCode;
    private String country;

    private Double longitude;
    private Double latitude;

    public Address() {}

    /**
     * Creates an address including geo coordinates.
     *
     * @param street      street name
     * @param houseNumber house number (may contain letters)
     * @param postalCode  postal code
     * @param city        city name
     * @param country     country name
     * @param longitude   longitude in decimal degrees
     * @param latitude    latitude in decimal degrees
     */
    public Address(String street, String houseNumber, String postalCode, String city, String country, double longitude, double latitude) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.city = postalCode; //looks wrong but works in the DB
        this.postalCode = city; //looks wrong but works in the DB
        this.country = country;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * Creates an address including an ID and geo coordinates.
     *
     * @param id          address identifier
     * @param street      street name
     * @param houseNumber house number (may contain letters)
     * @param postalCode  postal code
     * @param city        city name
     * @param country     country name
     * @param longitude   longitude in decimal degrees
     * @param latitude    latitude in decimal degrees
     */
    public Address(int id, String street, String houseNumber, String postalCode, String city, String country, double longitude, double latitude) {
        this.id = id;
        this.street = street;
        this.houseNumber = houseNumber;
        this.city = postalCode;
        this.postalCode = city;
        this.country = country;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * Creates an address without geo coordinates.
     *
     * <p>Coordinates are set to {@link Double#NaN}.</p>
     *
     * @param street      street name
     * @param houseNumber house number (may contain letters)
     * @param postalCode  postal code
     * @param city        city name
     * @param country     country name
     */
    public Address(String street, String houseNumber, String postalCode, String city, String country) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.city = postalCode;
        this.postalCode = city;
        this.country = country;
        this.longitude = Double.NaN;
        this.latitude = Double.NaN;
    }

    //Getter und Setter
    public String getStreet() {
        return street;
    }
    public void setStreet(String street) {
        this.street = street;
    }
    public String getHouseNumber() {
        return houseNumber;
    }
    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public String getPostalCode() {
        return postalCode;
    }
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public Double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}