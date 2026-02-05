package at.jku.se.gruppe2.infrastructure.persistence.statistics;

import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public class SensorReadingStatisticsRepository {

    public enum Granularity {
        HOUR("hour"),
        DAY("day"),
        WEEK("week"),
        MONTH("month");

        private final String pg;
        Granularity(String pg) { this.pg = pg; }
        public String pg() { return pg; }
    }

    public enum Aggregation {
        AVG, MIN, MAX, SUM, COUNT
    }

    /** KPI snapshot for a given scope/type/time-range. */
    public record Kpis(Double avg, Double min, Double max, long count) { }

    /** A single bucket point for charting. */
    public record BucketPoint(Instant bucketStart, Double value) { }

    /**
     * KPIs for a given sensor type (device_type.id) over a set of sensor IDs.
     *
     * @param sensorIds     sensor.device_id values
     * @param sensorTypeId  device_type.id (category SENSOR)
     */
    public Kpis getKpisForSensorsOfType(List<Integer> sensorIds,
                                        int sensorTypeId,
                                        Instant fromInclusive,
                                        Instant toExclusive) {

        if (sensorIds == null || sensorIds.isEmpty()) {
            return new Kpis(null, null, null, 0L);
        }

        InClause in = InClause.forInts(sensorIds);

        String sql = """
            SELECT
                AVG(sr.value)  AS avg_value,
                MIN(sr.value)  AS min_value,
                MAX(sr.value)  AS max_value,
                COUNT(*)       AS count_value
            FROM sensor_reading sr
            JOIN sensor s ON s.device_id = sr.sensor_id
            WHERE s.sensor_type_id = ?
              AND sr.time >= ?
              AND sr.time < ?
              AND sr.sensor_id IN %s
        """;

        Optional<Kpis> opt = JdbcTemplate.queryForObject(
                sql,
                ps -> {
                    int idx = 1;
                    ps.setInt(idx++, sensorTypeId);
                    ps.setTimestamp(idx++, Timestamp.from(fromInclusive));
                    ps.setTimestamp(idx++, Timestamp.from(toExclusive));
                    in.bind(ps, idx);
                },
                this::mapKpis
        );

        // Query always returns one row; but keep it safe.
        return opt.orElseGet(() -> new Kpis(null, null, null, 0L));
    }

    /**
     * Bucketed time-series for a given sensor type over a set of sensor IDs.
     *
     * @param aggregation AVG/MIN/MAX/SUM/COUNT
     * @return ordered by bucket ascending
     */
    public List<BucketPoint> getTimeSeriesForSensorsOfType(List<Integer> sensorIds,
                                                           int sensorTypeId,
                                                           Instant fromInclusive,
                                                           Instant toExclusive,
                                                           Granularity granularity,
                                                           Aggregation aggregation) {

        if (sensorIds == null || sensorIds.isEmpty()) {
            return Collections.emptyList();
        }

        InClause in = InClause.forInts(sensorIds);

        String aggSql = aggregationSql(aggregation);

        // date_trunc accepts text; we bind granularity as a string ("hour"/"day"/...)
        String sql = """
            SELECT
                date_trunc(?, sr.time) AS bucket,
                %s                     AS value
            FROM sensor_reading sr
            JOIN sensor s ON s.device_id = sr.sensor_id
            WHERE s.sensor_type_id = ?
              AND sr.time >= ?
              AND sr.time < ?
              AND sr.sensor_id IN %s
            GROUP BY bucket
            ORDER BY bucket ASC
        """.formatted(aggSql, in.sql());

        Optional<List<BucketPoint>> opt = JdbcTemplate.queryForMultipleObjects(
                sql,
                ps -> {
                    int idx = 1;
                    ps.setString(idx++, granularity.pg());
                    ps.setInt(idx++, sensorTypeId);
                    ps.setTimestamp(idx++, Timestamp.from(fromInclusive));
                    ps.setTimestamp(idx++, Timestamp.from(toExclusive));
                    in.bind(ps, idx);
                },
                this::mapBucketPoint
        );

        return opt.orElseGet(Collections::emptyList);
    }

    /**
     * Convenience: counts (events) per bucket for sensors of type.
     * Useful for MotionSensor etc. (either COUNT(*) or SUM(value) depending on how you encode events).
     */
    public List<BucketPoint> getEventCountSeriesForSensorsOfType(List<Integer> sensorIds,
                                                                 int sensorTypeId,
                                                                 Instant fromInclusive,
                                                                 Instant toExclusive,
                                                                 Granularity granularity) {
        return getTimeSeriesForSensorsOfType(
                sensorIds, sensorTypeId, fromInclusive, toExclusive, granularity, Aggregation.COUNT
        );
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private Kpis mapKpis(ResultSet rs) throws SQLException {
        Double avg = getNullableDouble(rs, "avg_value");
        Double min = getNullableDouble(rs, "min_value");
        Double max = getNullableDouble(rs, "max_value");
        long count = rs.getLong("count_value");
        return new Kpis(avg, min, max, count);
    }

    private BucketPoint mapBucketPoint(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("bucket");
        Instant bucket = (ts != null) ? ts.toInstant() : null;

        // COUNT(*) returns numeric/long; JDBC lets getDouble map it fine.
        Double value = getNullableDouble(rs, "value");
        return new BucketPoint(bucket, value);
    }

    private static Double getNullableDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    // -------------------------------------------------------------------------
    // SQL helpers
    // -------------------------------------------------------------------------

    private static String aggregationSql(Aggregation agg) {
        return switch (agg) {
            case AVG -> "AVG(sr.value)";
            case MIN -> "MIN(sr.value)";
            case MAX -> "MAX(sr.value)";
            case SUM -> "SUM(sr.value)";
            case COUNT -> "COUNT(*)";
        };
    }

    /**
     * Minimal helper to build "IN (?, ?, ...)" and bind ints.
     */
    private static final class InClause {
        private final List<Integer> values;

        private InClause(List<Integer> values) {
            this.values = values;
        }

        static InClause forInts(List<Integer> ints) {
            // Defensive copy in case caller mutates list later
            return new InClause(new ArrayList<>(ints));
        }

        String sql() {
            // "(?, ?, ?)"
            StringJoiner sj = new StringJoiner(", ", "(", ")");
            for (int i = 0; i < values.size(); i++) sj.add("?");
            return sj.toString();
        }

        void bind(PreparedStatement ps, int startIndex) throws SQLException {
            int idx = startIndex;
            for (Integer v : values) {
                ps.setInt(idx++, v);
            }
        }
    }

}
