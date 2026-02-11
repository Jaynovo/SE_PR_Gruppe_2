package at.jku.se.gruppe2.infrastructure.persistence.repository;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.DeviceType;
import at.jku.se.gruppe2.domain.model.device.actuator.*;
import at.jku.se.gruppe2.domain.model.device.sensor.*;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.sql.*;
import java.util.*;

/**
 * Repository for accessing and mutating {@link Device} entities and related tables.
 *
 * <p>This repository provides CRUD operations for {@code device} and helper operations for:</p>
 * <ul>
 *   <li>linking a device to a {@code sensor} or {@code actuator} type</li>
 *   <li>loading device types from {@code device_type}</li>
 *   <li>reading/writing {@code actuator_state}</li>
 * </ul>
 *
 * <p><b>Polymorphic mapping:</b> Depending on the device type category (SENSOR/ACTUATOR),
 * the repository instantiates specific subclasses such as {@link Thermometer} or
 * {@link VentilationActuator} based on the {@code device_type.label}.</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException}
 * by {@link JdbcTemplate}.</p>
 */
public class DeviceRepository {

    /**
     * Loads all devices belonging to a given room id (including joined device type info if present).
     *
     * @param roomId room id (foreign key {@code device.room_id})
     * @return list of devices in that room (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<Device> getDevicesByRoomId(int roomId) {
        String sql = """
            SELECT  d.id,
                    d.room_id,
                    d.label,
                    dt.id       AS type_id,
                    dt.label    AS type_label,
                    dt.category AS type_category,
                    dt.unit     AS type_unit
                FROM device d
                LEFT JOIN sensor s   ON s.device_id = d.id
                LEFT JOIN actuator a ON a.device_id = d.id
                LEFT JOIN device_type dt ON (dt.id = s.sensor_type_id AND s.device_id IS NOT NULL) 
                                         OR (dt.id = a.actuator_type_id AND a.device_id IS NOT NULL)
                WHERE d.room_id = ?
                ORDER BY d.id;
            """;

        Optional<List<Device>> devicesOpt = JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, roomId),
                this::mapDeviceWithRoomId  // Use the version that sets roomId
        );

        return devicesOpt.orElse(Collections.emptyList());
    }

    /**
     * Creates a {@code device} row for the given device and room.
     *
     * <p>The generated id is written back into the passed {@code device} instance.</p>
     *
     * @param device device to create (label is persisted)
     * @param room owning room (used for {@code room_id})
     * @return generated id if successful; {@code 0} if the id was not returned
     * @throws RuntimeException if a database/driver error occurs
     */
    public int createDevice(Device device, Room room) {
        String sql = """
                INSERT INTO device (room_id, label)
                VALUES (?, ?)
                RETURNING id;
                """;

        Optional<Integer> id = JdbcTemplate.queryForValue(
                sql,
                ps -> {
                    ps.setInt(1, room.getId());
                    ps.setString(2, device.getLabel());
                },
                rs -> rs.getInt("id")
        );

        // Set the ID inside the device object
        id.ifPresent(device::setId);

        return id.orElse(0);
    }

    /**
     * Updates the label of an existing device.
     *
     * @param device device to update (must have id and label set)
     * @return number of affected rows (typically {@code 1} if successful, {@code 0} if not found)
     * @throws RuntimeException if a database/driver error occurs
     */
    public int updateDevice(Device device) {
        String sql = """
                UPDATE device
                SET label = ?
                WHERE id = ?
                """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setString(1, device.getLabel());
                    ps.setInt(2, device.getId());
                }
        );
    }


    /**
     * Deletes a device by id.
     *
     * @param deviceId primary key of the device
     * @return number of affected rows
     * @throws RuntimeException if a database/driver error occurs
     */
    public int deleteDevice(int deviceId) {
        String sql = "DELETE FROM device WHERE id = ?";

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> ps.setInt(1, deviceId)
        );
    }

    /**
     * Deletes all devices belonging to a given room id.
     *
     * @param roomId room id (foreign key {@code device.room_id})
     * @return number of affected rows (may be {@code 0})
     * @throws RuntimeException if a database/driver error occurs
     */
    public int deleteDevicesByRoomId(int roomId) {
        String request = """
            DELETE FROM device
            WHERE room_id = ?;
            """;

        return JdbcTemplate.executeUpdate(
                request,
                ps -> ps.setInt(1, roomId)
        );
    }

    /**
     * Factory method that creates a concrete {@link Sensor} subtype based on the device type label.
     *
     * @param typeLabel value of {@code device_type.label}
     * @return a concrete sensor instance; returns an anonymous {@link Sensor} if no mapping matches
     */
    private Sensor createSensor(String typeLabel) {
        return switch (typeLabel) {
            case "Thermometer" -> new Thermometer();
            case "CO2Sensor" -> new CO2Sensor();
            case "NoiseSensor" -> new NoiseSensor();
            case "LightSensor" -> new LightSensor();
            case "HumiditySensor" -> new HumiditySensor();
            case "CatSensor" -> new CatSensor();
            case "MotionSensor" -> new MotionSensor();
            case "UtilityMeter" -> new UtilityMeterSensor();
            default -> new Sensor() {
            };
        };
    }

    /**
     * Factory method that creates a concrete {@link Actuator} subtype based on the device type label.
     *
     * @param typeLabel value of {@code device_type.label}
     * @return a concrete actuator instance; returns an anonymous {@link Actuator} if no mapping matches
     */
    private Actuator createActuator(String typeLabel) {
        return switch (typeLabel) {
            case "Ventilation" -> new VentilationActuator();
            case "AlarmSystem" -> new AlarmSystemActuator();
            case "SmartLightActuator" -> new SmartLightActuator();
            case "BlindsActuator" -> new BlindsActuator();
            case "SmartPlug" -> new SmartPlugActuator();
            default -> new Actuator() {
            };
        };
    }


    /**
     * Loads all device types of a given category from {@code device_type}.
     *
     * @param category category to filter by (cast to the DB enum {@code device_category})
     * @return list of device types (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<DeviceType> getDeviceTypesByCategory(Device.DeviceCategory category) {
        String sql = """
        SELECT id, category, label, unit
        FROM device_type
        WHERE category = CAST(? AS device_category)
        ORDER BY label;
    """;

        Optional<List<DeviceType>> typesOpt = JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setString(1, category.name()),
                rs -> {
                    DeviceType dt = new DeviceType();
                    dt.setId(rs.getInt("id"));
                    dt.setCategory(Device.DeviceCategory.valueOf(rs.getString("category")));
                    dt.setLabel(rs.getString("label"));
                    dt.setUnit(rs.getString("unit"));
                    return dt;
                }
        );

        return typesOpt.orElse(java.util.Collections.emptyList());
    }

    /**
     * Attaches a device to a sensor type by inserting into the {@code sensor} table.
     *
     * @param deviceId device id
     * @param sensorTypeId sensor type id (references {@code device_type.id})
     * @return number of affected rows (typically {@code 1})
     * @throws RuntimeException if a database/driver error occurs
     */
    public int attachSensor(int deviceId, int sensorTypeId) {
        String sql = """
        INSERT INTO sensor (device_id, sensor_type_id)
        VALUES (?, ?);
    """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setInt(1, deviceId);
                    ps.setInt(2, sensorTypeId);
                }
        );
    }

    /**
     * Attaches a device to an actuator type by inserting into the {@code actuator} table.
     *
     * @param deviceId device id
     * @param actuatorTypeId actuator type id (references {@code device_type.id})
     * @return number of affected rows (typically {@code 1})
     * @throws RuntimeException if a database/driver error occurs
     */
    public int attachActuator(int deviceId, int actuatorTypeId) {
        String sql = """
        INSERT INTO actuator (device_id, actuator_type_id)
        VALUES (?, ?);
    """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setInt(1, deviceId);
                    ps.setInt(2, actuatorTypeId);
                }
        );
    }

    /**
     * Returns the most recent state string for a given actuator (device id).
     *
     * @param actuatorDeviceId actuator id (stored as {@code actuator_state.actuator_id})
     * @return optional state string; empty if no state has been stored yet
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<String> getLatestActuatorState(int actuatorDeviceId) {
        String sql = """
        SELECT state
        FROM actuator_state
        WHERE actuator_id = ?
        ORDER BY time DESC
        LIMIT 1
    """;

        return JdbcTemplate.queryForValue(
                sql,
                ps -> ps.setInt(1, actuatorDeviceId),
                rs -> rs.getString("state")
        );
    }

    /**
     * Loads a single device by id (including joined device type info if present).
     *
     * @param deviceId primary key of the device
     * @return optional device (empty if not found)
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<Device> getDeviceById(int deviceId) {
        String sql = """
            SELECT  d.id,
                    d.room_id,
                    d.label,
                    dt.id       AS type_id,
                    dt.label    AS type_label,
                    dt.category AS type_category,
                    dt.unit     AS type_unit
                FROM device d
                LEFT JOIN sensor s   ON s.device_id = d.id
                LEFT JOIN actuator a ON a.device_id = d.id
                LEFT JOIN device_type dt ON (dt.id = s.sensor_type_id AND s.device_id IS NOT NULL)
                                             OR (dt.id = a.actuator_type_id AND a.device_id IS NOT NULL)
                WHERE d.id = ?;
            """;

        return JdbcTemplate.queryForObject(
                sql,
                ps -> ps.setInt(1, deviceId),
                this::mapDeviceWithRoomId
        );
    }

    /**
     * Loads all sensor devices that belong to a given home id.
     *
     * @param homeId home id referenced by {@code room.home_info}
     * @return list of sensor devices (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    public List<Device> getSensorDevicesByHomeId(int homeId) {
        String sql = """
        SELECT  d.id,
                d.room_id,
                d.label,
                dt.id       AS type_id,
                dt.label    AS type_label,
                dt.category AS type_category,
                dt.unit     AS type_unit
        FROM device d
        JOIN room r ON r.id = d.room_id
        JOIN sensor s ON s.device_id = d.id
        JOIN device_type dt ON dt.id = s.sensor_type_id
        WHERE r.home_info = ?
        ORDER BY d.id;
    """;

        return JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setInt(1, homeId),
                this::mapDeviceWithRoomId
        ).orElse(Collections.emptyList());
    }

    /**
     * Resolves the {@code device_type.id} for the given category and label.
     *
     * @param category device category (cast to DB enum {@code device_category})
     * @param label device type label
     * @return optional type id; empty if no matching type exists
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<Integer> getDeviceTypeIdByLabel(Device.DeviceCategory category, String label) {
        String sql = """
        SELECT id
        FROM device_type
        WHERE category = CAST(? AS device_category)
          AND label = ?
        LIMIT 1;
    """;

        return JdbcTemplate.queryForValue(
                sql,
                ps -> {
                    ps.setString(1, category.name());
                    ps.setString(2, label);
                },
                rs -> rs.getInt("id")
        );
    }

    /**
     * Maps a database row (including optional joined type columns) into a {@link Device} instance.
     *
     * <p>If {@code type_id} is {@code NULL}, an anonymous {@link Device} instance is created and
     * {@link Device#getType()} remains unset.</p>
     *
     * <p>If a type exists, a concrete sensor/actuator subclass is instantiated based on
     * {@code type_category} and {@code type_label}, and a {@link DeviceType} instance is created
     * and assigned via {@link Device#setType(DeviceType)}.</p>
     *
     * @param rs result set positioned at a valid row
     * @return mapped device (id and label set; type may be unset)
     * @throws SQLException if reading from the result set fails
     */
    private Device mapDevice(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String label = rs.getString("label");

        Integer typeId = (Integer) rs.getObject("type_id");
        String typeLabel = rs.getString("type_label");
        String typeCategory = rs.getString("type_category");
        String typeUnit = rs.getString("type_unit");

        Device device;

        if (typeId == null) {
            device = new Device() {};
        } else {
            Device.DeviceCategory category =
                    Device.DeviceCategory.valueOf(typeCategory);

            device = switch (category) {
                case SENSOR -> createSensor(typeLabel);
                case ACTUATOR -> createActuator(typeLabel);
            };

            //Create and set DeviceType
            DeviceType dt = new DeviceType();
            dt.setId(typeId);
            dt.setCategory(category);
            dt.setLabel(typeLabel);
            dt.setUnit(typeUnit);

            device.setType(dt);
        }

        device.setId(id);
        device.setLabel(label);
        return device;
    }

    /**
     * Variant of {@link #mapDevice(ResultSet)} that also assigns {@code room_id} to the mapped device.
     *
     * @param rs result set positioned at a valid row
     * @return mapped device with {@code roomId} set
     * @throws SQLException if reading from the result set fails
     */
    private Device mapDeviceWithRoomId(ResultSet rs) throws SQLException {
        Device device = mapDevice(rs);  // Reuse the mapping logic
        device.setRoomId(rs.getInt("room_id"));
        return device;
    }


    /**
     * Inserts a new actuator state record.
     *
     * @param actuatorDeviceId actuator id (device id) that the state belongs to
     * @param state state string to persist
     * @return number of affected rows (typically {@code 1})
     * @throws RuntimeException if a database/driver error occurs
     */
    public int insertActuatorState(int actuatorDeviceId, String state) {
        String sql = """
        INSERT INTO actuator_state (actuator_id, state)
        VALUES (?, ?)
    """;

        return JdbcTemplate.executeUpdate(
                sql,
                ps -> {
                    ps.setInt(1, actuatorDeviceId);
                    ps.setString(2, state);
                }
        );
    }
}
