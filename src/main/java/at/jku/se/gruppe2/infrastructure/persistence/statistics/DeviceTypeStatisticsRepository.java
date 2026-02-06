package at.jku.se.gruppe2.infrastructure.persistence.statistics;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.DeviceType;
import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DeviceTypeStatisticsRepository {
    public List<DeviceType> findSensorTypes() {
        return findByCategory(Device.DeviceCategory.SENSOR);
    }

    public List<DeviceType> findActuatorTypes() {
        return findByCategory(Device.DeviceCategory.ACTUATOR);
    }

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

    private List<DeviceType> findByCategory(Device.DeviceCategory category) {
        String sql = """
            SELECT * FROM device_type
            WHERE category = ?
            ORDER BY label
        """;

        return JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> ps.setString(1, category.name()),
                this::mapDeviceType
        ).orElse(Collections.emptyList());
    }

    private DeviceType mapDeviceType(ResultSet rs) throws SQLException {
        DeviceType dt = new DeviceType();
        dt.setId(rs.getInt("id"));
        dt.setCategory(Device.DeviceCategory.valueOf(rs.getString("category")));
        dt.setLabel(rs.getString("label"));
        dt.setUnit(rs.getString("unit"));
        return dt;
    }
}
