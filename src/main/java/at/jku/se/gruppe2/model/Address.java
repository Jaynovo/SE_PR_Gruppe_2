package at.jku.se.gruppe2.model;

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

    public Address(String street, String houseNumber, String city, String postalCode, String country, double longitude, double latitude) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Address(int id, String street , String houseNumber, String city, String postalCode, double longitude, String country, double latitude) {
        this.id = id;
        this.street = street;
        this.houseNumber = houseNumber;
        this.city = city;
        this.postalCode = postalCode;
        this.longitude = longitude;
        this.country = country;
        this.latitude = latitude;
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

    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}
