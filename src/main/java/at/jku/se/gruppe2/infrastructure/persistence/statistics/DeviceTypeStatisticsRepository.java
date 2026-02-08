package at.jku.se.gruppe2.infrastructure.persistence.statistics;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.DeviceType;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * Repository for querying {@link DeviceType} metadata used for statistics and visualization.
 *
 * <p>This repository provides read-only access to the {@code device_type} table and is primarily
 * used to retrieve available sensor and actuator types, as well as individual device type definitions.</p>
 *
 * <p><b>Categories:</b> Device types are separated into {@link Device.DeviceCategory#SENSOR} and
 * {@link Device.DeviceCategory#ACTUATOR} via the PostgreSQL enum {@code device_category}.</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException} by
 * {@link JdbcTemplate}.</p>
 */
public class DeviceTypeStatisticsRepository {
    public List<DeviceType> findSensorTypes() {
        return findByCategory(Device.DeviceCategory.SENSOR);
    }

    public List<DeviceType> findActuatorTypes() {
        return findByCategory(Device.DeviceCategory.ACTUATOR);
    }

    /**
     * Loads a {@link DeviceType} by its id.
     *
     * @param id device type id
     * @return optional device type; empty if not found
     * @throws RuntimeException if a database/driver error occurs
     */
    public Optional<DeviceType> findById(int id) {
        String sql = """
            SELECT * FROM device_type
            WHERE id = ?
        """;

        return JdbcTemplate.queryForObject(
                sql,
                ps -> ps.setInt(1, id),
                this::mapDeviceType
        );
    }

    /**
     * Loads all device types of a given category.
     *
     * @param category device category ({@link Device.DeviceCategory})
     * @return list of device types ordered by label (never {@code null})
     * @throws RuntimeException if a database/driver error occurs
     */
    private List<DeviceType> findByCategory(Device.DeviceCategory category) {
        String sql = """
            SELECT * FROM device_type
            WHERE category = ?::device_category
            ORDER BY label
        """;

        return JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setString(1, category.name()),
                this::mapDeviceType
        ).orElse(Collections.emptyList());
    }

    /**
     * Maps the current {@link ResultSet} row into a {@link DeviceType} domain object.
     *
     * @param rs result set positioned at a valid row
     * @return mapped device type
     * @throws SQLException if reading from the result set fails
     */
    private DeviceType mapDeviceType(ResultSet rs) throws SQLException {
        DeviceType dt = new DeviceType();
        dt.setId(rs.getInt("id"));
        dt.setCategory(Device.DeviceCategory.valueOf(rs.getString("category")));
        dt.setLabel(rs.getString("label"));
        dt.setUnit(rs.getString("unit"));
        return dt;
    }
}
