package at.jku.se.gruppe2.persistence;

import at.jku.se.gruppe2.model.*;

import java.util.Optional;

public class DeviceRepository {
    public DeviceRepository() {
    }
    public Optional<Device> findById(Integer id) {
        return Optional.ofNullable(null);
    }
}
