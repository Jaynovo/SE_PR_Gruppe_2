package at.jku.se.gruppe2.service;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.*;

import java.util.ArrayList;
import java.util.List;

public class UserBuildingService {
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final HomeRepository homeRepository;
    private final AddressRepository addressRepository;
    private final DeviceRepository deviceRepository;

    public UserBuildingService() {
        deviceRepository = new DeviceRepository();
        homeRepository = new HomeRepository();
        userRepository = new UserRepository();
        roomRepository = new RoomRepository();
        addressRepository = new AddressRepository();
    }
    public UserBuildingService(UserRepository userRepository, RoomRepository roomRepository, HomeRepository homeRepository, AddressRepository addressRepository, DeviceRepository deviceRepository) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.homeRepository = homeRepository;
        this.addressRepository = addressRepository;
        this.deviceRepository = deviceRepository;
    }

    public User buildUserByEmail(String email) {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found"));

        Address user_address = addressRepository.getAddressById(user.getAddress().getId()).orElse(null);
        user.setAddress(user_address);

        Home home = homeRepository.getHomeByUser(user).orElse(null);
        user.setHome(home);

        if (home != null) {
            Address home_address = addressRepository.getAddressById(home.getAddress().getId()).orElse(null);
            List<Room> rooms = new ArrayList<>();
            rooms = roomRepository.getAllRoomsByHome(home).orElse(new ArrayList<>());

            home.setAddress(home_address);
            home.setRooms(rooms);

            if (!home.getRooms().isEmpty()) {
                for (Room room : home.getRooms()) {
                    List<Device> devices = deviceRepository.getDevicesByRoomId(room.getId());
                    room.setDevices(devices);
                }
            }
        }
        return user;
    }

    private List<Room> getRooms(Home home) {
        return roomRepository.getAllRoomsByHome(home).orElse(new  ArrayList<>());
    }
}
