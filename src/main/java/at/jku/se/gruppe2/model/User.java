package at.jku.se.gruppe2.model;

public class User {

    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Home home;
    private Address address;
    private String avatarPath;

    public User() {}

    //Konstruktor ohne ID
    public User(String firstName, String lastName, String email, String password, Home home, Address address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.home = home;
        this.address = address;
    }

    //Konstruktor mit ID (laden aus DB)
    public User(int id, String firstName, String lastName, String email, String password, Home home, Address address) {
        this(firstName, lastName, email, password, home, address);
        this.id = id;
    }

    public User(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    public boolean userHasHome() {
        return (home != null);
    }

    //Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Home getHome() {
        return home;
    }
    public void setHome(Home home) {
        this.home = home;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getAvatarPath() {
        return avatarPath;
    }
    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName + " (" + email + ")";
    }
}
