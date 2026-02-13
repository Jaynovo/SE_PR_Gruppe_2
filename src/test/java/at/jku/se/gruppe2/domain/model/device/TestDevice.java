package at.jku.se.gruppe2.domain.model.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Device}.
 *
 * <p>These tests verify the convenience accessors that depend on {@link DeviceType}
 * and ensure null-safe behavior when no type is assigned.</p>
 */
class DeviceTest {

    /**
     * Minimal concrete device to allow instantiation of the abstract {@link Device} base class.
     */
    private static class TestDevice extends Device { }

    @Test
    void getCategory_returnsNullWhenTypeIsNull() {
        Device device = new TestDevice();

        assertNull(device.getCategory());
    }

    @Test
    void getTypeLabel_returnsNullWhenTypeIsNull() {
        Device device = new TestDevice();

        assertNull(device.getTypeLabel());
    }

    @Test
    void getUnit_returnsNullWhenTypeIsNull() {
        Device device = new TestDevice();

        assertNull(device.getUnit());
    }

    @Test
    void convenienceAccessors_returnValuesFromType() {
        Device device = new TestDevice();

        DeviceType type = new DeviceType();
        type.setCategory(Device.DeviceCategory.SENSOR);
        type.setLabel("CO2Sensor");
        type.setUnit("ppm");

        device.setType(type);

        assertEquals(Device.DeviceCategory.SENSOR, device.getCategory());
        assertEquals("CO2Sensor", device.getTypeLabel());
        assertEquals("ppm", device.getUnit());
    }

    @Test
    void settersAndGetters_storeValues() {
        Device device = new TestDevice();

        device.setId(42);
        device.setRoomId(7);
        device.setLabel("Living Room CO2");

        assertEquals(42, device.getId());
        assertEquals(7, device.getRoomId());
        assertEquals("Living Room CO2", device.getLabel());
    }
}
