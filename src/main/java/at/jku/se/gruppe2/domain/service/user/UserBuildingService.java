package at.jku.se.gruppe2.domain.service.user;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.home.Address;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.infrastructure.persistence.repository.AddressRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.*;

import java.util.*;

/**
 * Service responsible for building a fully-hydrated {@link User} aggregate from persistence.
 *
 * <p>This service loads a user and (if available) enriches it with:
 * <ul>
 *   <li>User address (if user has an address reference)</li>
 *   <li>User home (if assigned)</li>
 *   <li>Home address (if home has an address reference)</li>
 *   <li>Rooms in the home</li>
 *   <li>Devices per room</li>
 * </ul>
 */
public class UserBuildingService {
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final HomeRepository homeRepository;
    private final AddressRepository addressRepository;
    private final DeviceRepository deviceRepository;
    private final UserHomeRepository userHomeRepository;

    /**
     * Creates the service with default repository instances.
     */
    public UserBuildingService() {
        deviceRepository = new DeviceRepository();
        homeRepository = new HomeRepository();
        userRepository = new UserRepository();
        roomRepository = new RoomRepository();
        addressRepository = new AddressRepository();
        userHomeRepository = new UserHomeRepository();
    }

    /**
     * Creates the service with injected repositories.
     *
     * @param userRepository      repository used to load the user
     * @param roomRepository      repository used to load rooms for a home
     * @param homeRepository      repository used to load the home for a user
     * @param addressRepository   repository used to load address entities
     * @param deviceRepository    repository used to load devices per room
     * @param userHomeRepository  repository for user-home relationships (reserved for future use)
     */
    public UserBuildingService(UserRepository userRepository, RoomRepository roomRepository, HomeRepository homeRepository, AddressRepository addressRepository, DeviceRepository deviceRepository, UserHomeRepository userHomeRepository) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.homeRepository = homeRepository;
        this.addressRepository = addressRepository;
        this.deviceRepository = deviceRepository;
        this.userHomeRepository = userHomeRepository;
    }

    /**
     * Builds a {@link User} object by email and enriches it with related data from repositories.
     *
     * <p>Loaded structure:
     * <ol>
     *   <li>User by email</li>
     *   <li>User address (if referenced)</li>
     *   <li>Home associated with the user</li>
     *   <li>Home address (if referenced)</li>
     *   <li>Rooms of the home</li>
     *   <li>Devices for each room</li>
     * </ol>
     *
     * <p>Some data is optional and may remain {@code null}:
     * <ul>
     *   <li>Home (if user has no assigned home)</li>
     *   <li>Addresses (if IDs are missing or lookup fails)</li>
     * </ul></p>
     *
     * @param email email address used to look up the user
     * @return fully built {@link User} (without home if none exists)
     * @throws IllegalArgumentException if no user exists with the given email
     */
    public User buildUserByEmail(String email) {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found"));

        // Only fetch address if user has one
        if (user.getAddress() != null && user.getAddress().getId() > 0) {
            Address user_address = addressRepository.getAddressById(user.getAddress().getId()).orElse(null);
            user.setAddress(user_address);
        }

        Home home = homeRepository.getHomeByUser(user).orElse(null);
        user.setHome(home);

        if (home != null) {
            // Only fetch home address if home has one
            if (home.getAddress() != null && home.getAddress().getId() > 0) {
                Address home_address = addressRepository.getAddressById(home.getAddress().getId()).orElse(null);
                home.setAddress(home_address);
            }

            List<Room> rooms = roomRepository.getAllRoomsByHome(home).orElse(new ArrayList<>());
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
        return roomRepository.getAllRoomsByHome(home).orElse(new ArrayList<>());
    }
}