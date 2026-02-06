package at.jku.se.gruppe2.presentation.controller.statistics;

import at.jku.se.gruppe2.domain.model.device.DeviceType;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.domain.service.statistics.StatisticsService;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.DeviceTypeStatisticsRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.SensorReadingStatisticsRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.StatisticsScopeRepository;
import at.jku.se.gruppe2.infrastructure.security.Session;
import at.jku.se.gruppe2.presentation.controller.common.BaseController;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.List;

public class StatisticsDashboardController extends BaseController implements Initializable {
    @FXML private ComboBox<String> scopeBox;              // Home / Room / Device
    @FXML private ComboBox<Room> roomBox;                 // optional
    @FXML
    private ComboBox<DeviceType> metricBox;         // sensor types
    @FXML private ComboBox<String> rangeBox;              // 24h / 7d / 30d
    @FXML private Button refreshButton;

    @FXML private Label avgLabel;
    @FXML private Label minLabel;
    @FXML private Label maxLabel;
    @FXML private Label countLabel;

    @FXML private LineChart<String, Number> lineChart;

    private final HomeRepository homeRepo = new HomeRepository();

    private final StatisticsScopeRepository scopeRepo = new StatisticsScopeRepository();
    private final SensorReadingStatisticsRepository sensorStatsRepo = new SensorReadingStatisticsRepository();
    private final DeviceTypeStatisticsRepository deviceTypeRepo = new DeviceTypeStatisticsRepository();

    private final StatisticsService statsService =
            new StatisticsService(scopeRepo, sensorStatsRepo);

    private Home home;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User user = Session.getCurrentUser();
        if (user == null) {
            disableWithMessage("No user logged in.");
            return;
        }

        home = homeRepo.getHomeByUser(user).orElse(null);
        if (home == null) {
            disableWithMessage("No home available.");
            return;
        }

        // Scope + time range controls
        scopeBox.setItems(FXCollections.observableArrayList("Home", "Room"));
        rangeBox.setItems(FXCollections.observableArrayList("24h", "7d", "30d"));
        rangeBox.getSelectionModel().select("24h");

        // Metrics (sensor types)
        metricBox.setItems(FXCollections.observableArrayList(deviceTypeRepo.findSensorTypes()));
        if (!metricBox.getItems().isEmpty()) {
            metricBox.getSelectionModel().select(0);
        }

        // Default scope based on navigation context:
        // If a room is pre-selected in Session, start in Room scope. Otherwise start in Home scope.
        Room selectedRoom = Session.getSelectedRoom();
        if (selectedRoom != null) {
            roomBox.setItems(FXCollections.observableArrayList(selectedRoom));
            roomBox.getSelectionModel().select(selectedRoom);

            scopeBox.getSelectionModel().select("Room");
            roomBox.setDisable(false);
        } else {
            scopeBox.getSelectionModel().select("Home");
            roomBox.setDisable(true);
        }

        // Reactive refresh
        scopeBox.valueProperty().addListener((obs, oldV, newV) -> {
            roomBox.setDisable(!"Room".equals(newV));
            refresh();
        });
        roomBox.valueProperty().addListener((obs, o, n) -> {
            if ("Room".equals(scopeBox.getValue())) refresh();
        });
        metricBox.valueProperty().addListener((obs, o, n) -> refresh());
        rangeBox.valueProperty().addListener((obs, o, n) -> refresh());

        refresh();
    }

    @FXML
    public void handleRefresh() {
        refresh();
    }

    private void refresh() {
        DeviceType metric = metricBox.getValue();
        if (metric == null) return;

        StatisticsService.DashboardScope scope = resolveScope();
        if (scope == null) {
            clearStats();
            return;
        }

        TimeConfig tc = computeTimeConfig(rangeBox.getValue());

        var kpis = statsService.getSensorKpis(
                scope,
                metric.getId(),
                new StatisticsService.TimeRange(tc.from, tc.to)
        );

        String unit = (metric.getUnit() == null || metric.getUnit().isBlank()) ? "" : " " + metric.getUnit();
        avgLabel.setText(format(kpis.avg()) + unit);
        minLabel.setText(format(kpis.min()) + unit);
        maxLabel.setText(format(kpis.max()) + unit);
        countLabel.setText(String.valueOf(kpis.count()));

        var points = statsService.getSensorSeries(
                scope,
                metric.getId(),
                new StatisticsService.TimeRange(tc.from, tc.to),
                tc.granularity,
                SensorReadingStatisticsRepository.Aggregation.AVG
        );

        renderLineChart(metric.getLabel(), points);
    }

    private StatisticsService.DashboardScope resolveScope() {
        String scopeStr = scopeBox.getValue();
        if ("Home".equals(scopeStr)) {
            return StatisticsService.DashboardScope.home(home.getId());
        }
        if ("Room".equals(scopeStr)) {
            Room room = roomBox.getValue();
            if (room == null) return null;
            return StatisticsService.DashboardScope.room(room.getId());
        }
        return null;
    }

    private void renderLineChart(String seriesName,
                                 java.util.List<SensorReadingStatisticsRepository.BucketPoint> points) {
        lineChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(seriesName);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

        for (var p : points) {
            if (p.bucketStart() == null || p.value() == null) continue;
            series.getData().add(new XYChart.Data<>(fmt.format(p.bucketStart()), p.value()));
        }

        lineChart.getData().add(series);
    }

    private static String format(Double v) {
        if (v == null) return "-";
        return String.format(Locale.US, "%.2f", v);
    }

    private void disableWithMessage(String msg) {
        avgLabel.setText(msg);
        minLabel.setText("-");
        maxLabel.setText("-");
        countLabel.setText("-");
        countLabel.setText("-");
        lineChart.setDisable(true);
        scopeBox.setDisable(true);
        roomBox.setDisable(true);
        metricBox.setDisable(true);
        rangeBox.setDisable(true);
    }

    private void clearStats() {
        avgLabel.setText("-");
        minLabel.setText("-");
        maxLabel.setText("-");
        countLabel.setText("-");
        lineChart.getData().clear();
    }

    private static class TimeConfig {
        Instant from;
        Instant to;
        SensorReadingStatisticsRepository.Granularity granularity;
    }

    private static TimeConfig computeTimeConfig(String range) {
        TimeConfig tc = new TimeConfig();
        tc.to = Instant.now();

        if ("7d".equals(range)) {
            tc.from = tc.to.minus(Duration.ofDays(7));
            tc.granularity = SensorReadingStatisticsRepository.Granularity.DAY;
        } else if ("30d".equals(range)) {
            tc.from = tc.to.minus(Duration.ofDays(30));
            tc.granularity = SensorReadingStatisticsRepository.Granularity.DAY;
        } else {
            tc.from = tc.to.minus(Duration.ofHours(24));
            tc.granularity = SensorReadingStatisticsRepository.Granularity.HOUR;
        }
        return tc;
    }
}
