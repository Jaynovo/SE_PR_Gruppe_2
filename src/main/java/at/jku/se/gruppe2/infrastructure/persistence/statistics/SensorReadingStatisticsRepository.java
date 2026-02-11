package at.jku.se.gruppe2.infrastructure.persistence.statistics;

import at.jku.se.gruppe2.infrastructure.persistence.config.JdbcTemplate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;


/**
 * Repository for aggregated statistics over sensor readings.
 *
 * <p>This class queries {@code sensor_reading} joined with {@code sensor} to provide:</p>
 * <ul>
 *   <li>KPI snapshots (avg/min/max/count) for a sensor type across a set of sensor ids</li>
 *   <li>Bucketed time-series values for charting (by hour/day/week/month)</li>
 * </ul>
 *
 * <p><b>Scope model:</b> All queries operate on a set of sensor device ids ({@code sensor.device_id})
 * and filter by a specific sensor type id ({@code sensor.sensor_type_id}).</p>
 *
 * <p><b>Time window:</b> All queries use a half-open interval: {@code [fromInclusive, toExclusive)}.</p>
 *
 * <p><b>Empty input behavior:</b> If {@code sensorIds} is null/empty, methods return safe empty results
 * (no DB query is executed).</p>
 *
 * <p><b>Error handling:</b> SQL/connection errors are wrapped in {@link RuntimeException} by
 * {@link JdbcTemplate}.</p>
 */
public class SensorReadingStatisticsRepository {

    /**
     * Supported bucket sizes for time-series aggregation.
     *
     * <p>The {@link #pg()} value is passed to PostgreSQL {@code date_trunc(text, timestamp)}.</p>
     */
    public enum Granularity {
        HOUR("hour"),
        DAY("day"),
        WEEK("week"),
        MONTH("month");

        private final String pg;
        Granularity(String pg) { this.pg = pg; }

        /**
         * Returns the PostgreSQL {@code date_trunc} granularity string.
         *
         * @return "hour", "day", "week", or "month"
         */
        public String pg() { return pg; }
    }


    /**
     * Supported SQL aggregations for time-series queries.
     */
    public enum Aggregation {
        AVG, MIN, MAX, SUM, COUNT
    }


    /**
     * KPI snapshot for a given scope/type/time-range.
     *
     * @param avg   average of {@code sr.value} (null if no values)
     * @param min   minimum of {@code sr.value} (null if no values)
     * @param max   maximum of {@code sr.value} (null if no values)
     * @param count number of readings in the window
     */
    public record Kpis(Double avg, Double min, Double max, long count) { }

    /**
     * A single bucket point for charting.
     *
     * @param bucketStart start timestamp of the bucket (as returned by {@code date_trunc})
     * @param value aggregated value for that bucket (may be null if the aggregation yields NULL)
     */
    public record BucketPoint(Instant bucketStart, Double value) { }

    /**
     * Computes KPI values (avg/min/max/count) for a given sensor type over a set of sensor ids.
     *
     * <p>The query filters by:</p>
     * <ul>
     *   <li>{@code s.sensor_type_id = sensorTypeId}</li>
     *   <li>{@code sr.time >= fromInclusive AND sr.time < toExclusive}</li>
     *   <li>{@code sr.sensor_id IN (sensorIds...)}</li>
     * </ul>
     *
     * @param sensorIds      sensor.device_id values to include
     * @param sensorTypeId   device_type.id for a sensor type (category SENSOR)
     * @param fromInclusive  inclusive start of the time window
     * @param toExclusive    exclusive end of the time window
     * @return KPI snapshot; if {@code sensorIds} is empty, returns {@code (null, null, null, 0)}
     * @throws RuntimeException if a database/driver error occurs
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
        """.formatted(in.sql());

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
     * Computes a bucketed time-series for a given sensor type over a set of sensor ids.
     *
     * <p>Buckets are generated using {@code date_trunc(granularity, sr.time)} and the chosen aggregation.</p>
     *
     * @param sensorIds      sensor.device_id values to include
     * @param sensorTypeId   device_type.id for a sensor type (category SENSOR)
     * @param fromInclusive  inclusive start of the time window
     * @param toExclusive    exclusive end of the time window
     * @param granularity    bucket granularity (hour/day/week/month)
     * @param aggregation    aggregation to apply to the bucket (AVG/MIN/MAX/SUM/COUNT)
     * @return list of bucket points ordered by bucket ascending; empty if {@code sensorIds} is empty
     * @throws RuntimeException if a database/driver error occurs
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
     * Convenience method for event-like sensors: returns {@link Aggregation#COUNT} per bucket.
     *
     * @param sensorIds      sensor.device_id values to include
     * @param sensorTypeId   device_type.id for a sensor type (category SENSOR)
     * @param fromInclusive  inclusive start of the time window
     * @param toExclusive    exclusive end of the time window
     * @param granularity    bucket granularity (hour/day/week/month)
     * @return list of bucket points ordered by bucket ascending; empty if {@code sensorIds} is empty
     * @throws RuntimeException if a database/driver error occurs
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

    /**
     * Maps a KPI result row into a {@link Kpis} record.
     *
     * @param rs result set positioned at the KPI row
     * @return KPI snapshot
     * @throws SQLException if reading from the result set fails
     */
    private Kpis mapKpis(ResultSet rs) throws SQLException {
        Double avg = getNullableDouble(rs, "avg_value");
        Double min = getNullableDouble(rs, "min_value");
        Double max = getNullableDouble(rs, "max_value");
        long count = rs.getLong("count_value");
        return new Kpis(avg, min, max, count);
    }

    /**
     * Maps a bucket row into a {@link BucketPoint}.
     *
     * @param rs result set positioned at a bucket row
     * @return bucket point
     * @throws SQLException if reading from the result set fails
     */
    private BucketPoint mapBucketPoint(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("bucket");
        Instant bucket = (ts != null) ? ts.toInstant() : null;

        // COUNT(*) returns numeric/long; JDBC lets getDouble map it fine.
        Double value = getNullableDouble(rs, "value");
        return new BucketPoint(bucket, value);
    }

    /**
     * Reads a double value from the given column and returns {@code null} if the database value is NULL.
     *
     * @param rs result set
     * @param col column name
     * @return boxed Double or null
     * @throws SQLException if reading from the result set fails
     */
    private static Double getNullableDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    // -------------------------------------------------------------------------
    // SQL helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the SQL fragment used for the selected aggregation.
     *
     * @param agg aggregation to use
     * @return SQL fragment such as {@code "AVG(sr.value)"} or {@code "COUNT(*)"}
     */
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
     * Helper to build an {@code IN (?, ?, ...)} clause and bind integer parameters safely.
     *
     * <p>This avoids string-concatenating raw numbers into SQL while still supporting variable-length lists.</p>
     */
    private static final class InClause {
        private final List<Integer> values;

        private InClause(List<Integer> values) {
            this.values = values;
        }

        /**
         * Creates an {@link InClause} for integer values.
         *
         * @param ints integer list
         * @return in-clause helper with a defensive copy of the list
         */
        static InClause forInts(List<Integer> ints) {
            // Defensive copy in case caller mutates list later
            return new InClause(new ArrayList<>(ints));
        }

        /**
         * Builds the SQL placeholder list, e.g. {@code "(?, ?, ?)"}.
         *
         * @return SQL placeholder list
         */
        String sql() {
            // "(?, ?, ?)"
            StringJoiner sj = new StringJoiner(", ", "(", ")");
            for (int i = 0; i < values.size(); i++) sj.add("?");
            return sj.toString();
        }

        /**
         * Binds all values to the prepared statement starting at the given index.
         *
         * @param ps prepared statement
         * @param startIndex 1-based start index for parameters
         * @throws SQLException if binding fails
         */
        void bind(PreparedStatement ps, int startIndex) throws SQLException {
            int idx = startIndex;
            for (Integer v : values) {
                ps.setInt(idx++, v);
            }
        }
    }

}
