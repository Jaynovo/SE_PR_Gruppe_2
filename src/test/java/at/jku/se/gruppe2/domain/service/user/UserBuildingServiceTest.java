package at.jku.se.gruppe2.domain.service.user;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.home.Address;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserBuildingService}.
 *
 * <p>Minimal coverage: user-not-found, user-without-home, and full hydration (home/rooms/devices).</p>
 */
class UserBuildingServiceTest {

    // -----------------------------
    // Fakes (no Mockito)
    // -----------------------------

    private static class FakeUserRepo extends UserRepository {
        Optional<User> userByEmail = Optional.empty();
        int findCalls = 0;

        @Override
        public Optional<User> findUserByEmail(String email) {
            findCalls++;
            return userByEmail;
        }
    }

    private static class FakeHomeRepo extends HomeRepository {
        Optional<Home> homeByUser = Optional.empty();
        int calls = 0;

        @Override
        public Optional<Home> getHomeByUser(User user) {
            calls++;
            return homeByUser;
        }
    }

    private static class FakeRoomRepo extends RoomRepository {
        Optional<List<Room>> roomsByHome = Optional.empty();
        int calls = 0;

        @Override
        public Optional<List<Room>> getAllRoomsByHome(Home home) {
            calls++;
            return roomsByHome;
        }
    }

    private static class FakeAddressRepo extends AddressRepository {
        Map<Integer, Address> byId = new HashMap<>();
        int calls = 0;

        @Override
        public Optional<Address> getAddressById(int id) {
            calls++;
            return Optional.ofNullable(byId.get(id));
        }
    }

    private static class FakeDeviceRepo extends DeviceRepository {
        Map<Integer, List<Device>> devicesByRoom = new HashMap<>();
        int calls = 0;

        @Override
        public List<Device> getDevicesByRoomId(int roomId) {
            calls++;
            return devicesByRoom.getOrDefault(roomId, Collections.emptyList());
        }
    }

    // not used in logic currently, but required by constructor
    private static class FakeUserHomeRepo extends UserHomeRepository { }

    private static Device dev(int id, String label) {
        Device d = new Device() {};
        d.setId(id);
        d.setLabel(label);
        return d;
    }

    // -----------------------------
    // Tests (minimal)
    // -----------------------------

    @Test
    void buildUserByEmail_throwsWhenUserNotFound() {
        FakeUserRepo userRepo = new FakeUserRepo();
        FakeRoomRepo roomRepo = new FakeRoomRepo();
        FakeHomeRepo homeRepo = new FakeHomeRepo();
        FakeAddressRepo addrRepo = new FakeAddressRepo();
        FakeDeviceRepo deviceRepo = new FakeDeviceRepo();
        FakeUserHomeRepo userHomeRepo = new FakeUserHomeRepo();

        userRepo.userByEmail = Optional.empty();

        UserBuildingService svc = new UserBuildingService(
                userRepo, roomRepo, homeRepo, addrRepo, deviceRepo, userHomeRepo
        );

        assertThrows(IllegalArgumentException.class,
                () -> svc.buildUserByEmail("x@example.com"));

        assertEquals(1, userRepo.findCalls);
        assertEquals(0, homeRepo.calls);
        assertEquals(0, roomRepo.calls);
        assertEquals(0, addrRepo.calls);
        assertEquals(0, deviceRepo.calls);
    }

    @Test
    void buildUserByEmail_userFoundButNoHome_doesNotLoadRoomsOrDevices() {
        FakeUserRepo userRepo = new FakeUserRepo();
        FakeRoomRepo roomRepo = new FakeRoomRepo();
        FakeHomeRepo homeRepo = new FakeHomeRepo();
        FakeAddressRepo addrRepo = new FakeAddressRepo();
        FakeDeviceRepo deviceRepo = new FakeDeviceRepo();
        FakeUserHomeRepo userHomeRepo = new FakeUserHomeRepo();

        User u = new User();
        u.setId(1);
        u.setEmail("a@b.com");

        // user has no address reference => should not call address repo
        u.setAddress(null);

        userRepo.userByEmail = Optional.of(u);
        homeRepo.homeByUser = Optional.empty(); // no home

        UserBuildingService svc = new UserBuildingService(
                userRepo, roomRepo, homeRepo, addrRepo, deviceRepo, userHomeRepo
        );

        User built = svc.buildUserByEmail("a@b.com");

        assertSame(u, built);
        assertNull(built.getHome());

        assertEquals(1, userRepo.findCalls);
        assertEquals(1, homeRepo.calls);
        assertEquals(0, roomRepo.calls);
        assertEquals(0, deviceRepo.calls);
        assertEquals(0, addrRepo.calls);
    }

    @Test
    void buildUserByEmail_fullHydration_loadsAddressesRoomsAndDevices() {
        FakeUserRepo userRepo = new FakeUserRepo();
        FakeRoomRepo roomRepo = new FakeRoomRepo();
        FakeHomeRepo homeRepo = new FakeHomeRepo();
        FakeAddressRepo addrRepo = new FakeAddressRepo();
        FakeDeviceRepo deviceRepo = new FakeDeviceRepo();
        FakeUserHomeRepo userHomeRepo = new FakeUserHomeRepo();

        // --- user with address id>0
        Address userAddrRef = new Address();
        userAddrRef.setId(10);

        User u = new User();
        u.setId(1);
        u.setEmail("a@b.com");
        u.setAddress(userAddrRef);

        // address repo returns actual hydrated address
        Address userAddr = new Address();
        userAddr.setId(10);
        userAddr.setStreet("UserStreet");
        addrRepo.byId.put(10, userAddr);

        userRepo.userByEmail = Optional.of(u);

        // --- home with address id>0
        Address homeAddrRef = new Address();
        homeAddrRef.setId(20);

        Home h = new Home();
        h.setId(99);
        h.setAddress(homeAddrRef);

        Address homeAddr = new Address();
        homeAddr.setId(20);
        homeAddr.setStreet("HomeStreet");
        addrRepo.byId.put(20, homeAddr);

        homeRepo.homeByUser = Optional.of(h);

        // --- rooms
        Room r1 = new Room(); r1.setId(100);
        Room r2 = new Room(); r2.setId(200);
        roomRepo.roomsByHome = Optional.of(List.of(r1, r2));

        // --- devices per room
        deviceRepo.devicesByRoom.put(100, List.of(dev(1, "D1"), dev(2, "D2")));
        deviceRepo.devicesByRoom.put(200, List.of(dev(3, "D3")));

        UserBuildingService svc = new UserBuildingService(
                userRepo, roomRepo, homeRepo, addrRepo, deviceRepo, userHomeRepo
        );

        User built = svc.buildUserByEmail("a@b.com");

        assertSame(u, built);

        // user address hydrated
        assertNotNull(built.getAddress());
        assertEquals("UserStreet", built.getAddress().getStreet());

        // home set and home address hydrated
        assertNotNull(built.getHome());
        assertEquals(99, built.getHome().getId());
        assertNotNull(built.getHome().getAddress());
        assertEquals("HomeStreet", built.getHome().getAddress().getStreet());

        // rooms set
        assertNotNull(built.getHome().getRooms());
        assertEquals(2, built.getHome().getRooms().size());

        // devices loaded for each room
        assertEquals(2, built.getHome().getRooms().get(0).getDevices().size());
        assertEquals(1, built.getHome().getRooms().get(1).getDevices().size());

        assertEquals(1, userRepo.findCalls);
        assertEquals(1, homeRepo.calls);
        assertEquals(1, roomRepo.calls);
        assertEquals(2, deviceRepo.calls); // two rooms => two calls
        assertEquals(2, addrRepo.calls);   // user addr + home addr
    }
}