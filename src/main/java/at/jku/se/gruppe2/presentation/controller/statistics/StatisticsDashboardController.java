package at.jku.se.gruppe2.presentation.controller.statistics;

import at.jku.se.gruppe2.domain.model.device.DeviceType;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.domain.service.statistics.DemoDataSeeder;
import at.jku.se.gruppe2.domain.service.statistics.StatisticsService;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.RoomRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.SensorReadingRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.DeviceTypeStatisticsRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.SensorReadingStatisticsRepository;
import at.jku.se.gruppe2.infrastructure.persistence.statistics.StatisticsScopeRepository;
import at.jku.se.gruppe2.infrastructure.security.Session;
import at.jku.se.gruppe2.presentation.controller.common.BaseController;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

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
    @FXML private ComboBox<DeviceType> metricBox;         // sensor types
    @FXML private ComboBox<String> rangeBox;              // 24h / 7d / 30d
    @FXML private ComboBox<DemoDataSeeder.IntervalPreset> seedRangeBox;
    @FXML private VBox seedCard;

    @FXML private Button refreshButton;
    @FXML private Button seedButton;

    @FXML private Label avgLabel;
    @FXML private Label minLabel;
    @FXML private Label maxLabel;
    @FXML private Label countLabel;
    @FXML private Label seedHintLabel;

    @FXML private StackPane chartContainer;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private LineChart<Number, Number> lineChart;

    private final HomeRepository homeRepo = new HomeRepository();

    private final StatisticsScopeRepository scopeRepo = new StatisticsScopeRepository();
    private final SensorReadingStatisticsRepository sensorStatsRepo = new SensorReadingStatisticsRepository();
    private final DeviceTypeStatisticsRepository deviceTypeRepo = new DeviceTypeStatisticsRepository();
    private final DeviceRepository deviceRepo = new DeviceRepository();
    private final SensorReadingRepository readingRepo = new SensorReadingRepository();
    private final DemoDataSeeder demoSeeder = new DemoDataSeeder(deviceRepo, new RoomRepository(), readingRepo);
    private final RoomRepository roomRepo = new RoomRepository();

    private final StatisticsService statsService =
            new StatisticsService(scopeRepo, sensorStatsRepo);

    private double dataMinX, dataMaxX, dataMinY, dataMaxY;
    private boolean dataBoundsValid;


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

        List<Room> rooms = roomRepo.getAllRoomsByHomeId(home.getId());
        roomBox.setItems(FXCollections.observableArrayList(rooms));

        roomBox.setCellFactory(cb -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getRoomLabel());
            }
        });
        roomBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getRoomLabel());
            }
        });

        if (!rooms.isEmpty()) {
            roomBox.getSelectionModel().select(0);
        }

        seedRangeBox.setItems(FXCollections.observableArrayList(DemoDataSeeder.IntervalPreset.values()));
        seedRangeBox.getSelectionModel().select(DemoDataSeeder.IntervalPreset.LAST_7D);

        seedButton.setOnAction(e -> {
            demoSeeder.seedIfMissing(home.getId(), seedRangeBox.getValue().duration);
            refresh();
        });

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
            rooms.stream()
                    .filter(r -> r.getId() == selectedRoom.getId())
                    .findFirst()
                    .ifPresent(r -> roomBox.getSelectionModel().select(r));

            scopeBox.getSelectionModel().select("Room");
            roomBox.setDisable(false);
        } else {
            scopeBox.getSelectionModel().select("Home");
            roomBox.setDisable(true);
        }

        scopeBox.valueProperty().addListener((obs, oldV, newV) -> {
            boolean roomScope = "Room".equals(newV);
            roomBox.setDisable(!roomScope);

            // if entering Room scope and nothing selected, select first
            if (roomScope && roomBox.getValue() == null && !roomBox.getItems().isEmpty()) {
                roomBox.getSelectionModel().select(0);
            }
            refresh();
        });

        roomBox.valueProperty().addListener((obs, o, n) -> {
            if ("Room".equals(scopeBox.getValue())) refresh();
        });
        metricBox.valueProperty().addListener((obs, o, n) -> refresh());
        rangeBox.valueProperty().addListener((obs, o, n) -> refresh());

        if (isDashboardEmpty()) clearStats();

        boolean show = isDashboardEmpty();
        seedButton.setVisible(show);
        seedButton.setManaged(show);
        seedRangeBox.setVisible(show);
        seedRangeBox.setManaged(show);

        installInteractions();
        refresh();
    }

    @FXML
    public void handleRefresh() {
        refresh();
    }

    private void handleSeed() {
        var preset = seedRangeBox.getValue();
        if (preset == null) preset = DemoDataSeeder.IntervalPreset.LAST_7D;

        demoSeeder.seedIfMissing(home.getId(), preset.duration);

        refresh();                    // rerender KPIs + chart
        updateSeedControlsVisibility(); // hide CTA afterwards
    }

    private void refresh() {
        if (seedButton != null) updateSeedControlsVisibility();
        if (isDashboardEmpty()) {
            clearStats();
            return;
        }

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

        var avgPoints = statsService.getSensorSeries(
                scope,
                metric.getId(),
                new StatisticsService.TimeRange(tc.from, tc.to),
                tc.granularity,
                SensorReadingStatisticsRepository.Aggregation.AVG
        );

        var minPoints = statsService.getSensorSeries(
                scope,
                metric.getId(),
                new StatisticsService.TimeRange(tc.from, tc.to),
                tc.granularity,
                SensorReadingStatisticsRepository.Aggregation.MIN
        );

        var maxPoints = statsService.getSensorSeries(
                scope,
                metric.getId(),
                new StatisticsService.TimeRange(tc.from, tc.to),
                tc.granularity,
                SensorReadingStatisticsRepository.Aggregation.MAX
        );
        applyTickUnit(tc);
        renderLineChartBands(metric.getLabel(), minPoints, avgPoints, maxPoints);
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

    private void renderLineChartBands(String metricLabel,
                                      List<SensorReadingStatisticsRepository.BucketPoint> minPoints,
                                      List<SensorReadingStatisticsRepository.BucketPoint> avgPoints,
                                      List<SensorReadingStatisticsRepository.BucketPoint> maxPoints) {

        var minSeries = buildSeries(metricLabel + " (Min)", minPoints);
        var avgSeries = buildSeries(metricLabel + " (Avg)", avgPoints);
        var maxSeries = buildSeries(metricLabel + " (Max)", maxPoints);

        lineChart.getData().setAll(minSeries, avgSeries, maxSeries);

        resetViewToData();
        fixLegendColors();

        for (int i = 0; i < lineChart.getData().size(); i++) {
            System.out.println(i + ": " + lineChart.getData().get(i).getName());
        }
        javafx.application.Platform.runLater(() -> {
            applySeriesClass(minSeries, "series-min");
            applySeriesClass(avgSeries, "series-avg");
            applySeriesClass(maxSeries, "series-max");
        });

        updateDataBoundsFromChart();
        configureTimeAxis();
        installTooltips(avgSeries);

        lineChart.setCreateSymbols(false);
    }

    private XYChart.Series<Number, Number> buildSeries(
            String name,
            List<SensorReadingStatisticsRepository.BucketPoint> points) {

        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        s.setName(name);

        for (var p : points) {
            if (p.bucketStart() == null || p.value() == null) continue;
            long x = p.bucketStart().toEpochMilli();
            s.getData().add(new XYChart.Data<>(x, p.value()));
        }
        return s;
    }

    private void applySeriesClass(XYChart.Series<Number, Number> series, String cssClass) {
        var node = series.getNode();
        if (node != null && !node.getStyleClass().contains(cssClass)) {
            node.getStyleClass().add(cssClass);
        }
    }

    private void configureTimeAxis() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

        xAxis.setTickLabelFormatter(new javafx.util.StringConverter<>() {
            @Override public String toString(Number object) {
                if (object == null) return "";
                return fmt.format(Instant.ofEpochMilli(object.longValue()));
            }
            @Override public Number fromString(String string) { return 0; }
        });
    }

    private void installTooltips(XYChart.Series<Number, Number> series) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

        for (XYChart.Data<Number, Number> d : series.getData()) {
            d.nodeProperty().addListener((obs, old, node) -> {
                if (node == null) return;

                Instant t = Instant.ofEpochMilli(d.getXValue().longValue());
                String text = fmt.format(t) + "\nValue: " + format(d.getYValue().doubleValue());

                Tooltip tip = new Tooltip(text);
                Tooltip.install(node, tip);

                node.setOnMouseEntered(e -> node.setStyle("-fx-scale-x: 1.4; -fx-scale-y: 1.4;"));
                node.setOnMouseExited(e -> node.setStyle(""));
            });
        }
    }

    private Rectangle zoomRect;
    private Point2D dragAnchor;      // start point in container coords
    private Point2D lastPan;         // last point for panning

    private void installInteractions() {
        // --- create overlay rectangle once ---
        if (zoomRect == null) {
            zoomRect = new Rectangle();
            zoomRect.setManaged(false);
            zoomRect.setVisible(false);
            zoomRect.getStyleClass().add("zoom-rect");
            chartContainer.getChildren().add(zoomRect);
            StackPane.setAlignment(zoomRect, javafx.geometry.Pos.TOP_LEFT);
        }

        // --- Scroll zoom (addEventFilter so it doesn't override anything) ---
        chartContainer.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            // zoom only if cursor is over plot area-ish; still okay if not
            double factor = (e.getDeltaY() > 0) ? 0.9 : 1.1;

            // If auto-ranging, lock it and initialize from data so zoom actually works
            if (xAxis.isAutoRanging() || yAxis.isAutoRanging()) {
                xAxis.setAutoRanging(false);
                yAxis.setAutoRanging(false);
                initBoundsFromCurrentData();
            }

            double mouseXVal = xAxis.getValueForDisplay(toAxisX(e.getX())).doubleValue();
            double mouseYVal = yAxis.getValueForDisplay(toAxisY(e.getY())).doubleValue();

            zoomAxisAround(xAxis, mouseXVal, factor);
            //zoomAxisAround(yAxis, mouseYVal, factor);
            clampAxesToData();
            e.consume();
        });

        // --- Mouse pressed: decide mode (zoom vs pan) ---
        chartContainer.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (e.isSecondaryButtonDown()) return;

            if (e.isPrimaryButtonDown()) {
                if (e.isShiftDown()) {
                    // PAN start
                    lastPan = new Point2D(e.getX(), e.getY());

                    if (xAxis.isAutoRanging() || yAxis.isAutoRanging()) {
                        xAxis.setAutoRanging(false);
                        yAxis.setAutoRanging(false);
                        initBoundsFromCurrentData();
                    }

                } else {
                    // ZOOM-RECT start
                    dragAnchor = new Point2D(e.getX(), e.getY());
                    zoomRect.setX(dragAnchor.getX());
                    zoomRect.setY(dragAnchor.getY());
                    zoomRect.setWidth(0);
                    zoomRect.setHeight(0);
                    zoomRect.setVisible(true);
                }
                e.consume();
            }
        });

        // --- Mouse dragged: perform zoom rectangle or pan ---
        chartContainer.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            if (!e.isPrimaryButtonDown()) return;

            if (e.isShiftDown()) {
                // PAN
                if (lastPan == null) return;

                double dx = e.getX() - lastPan.getX();
                double dy = e.getY() - lastPan.getY();
                lastPan = new Point2D(e.getX(), e.getY());

                // convert pixel shift to value shift
                double xShift = xAxis.getValueForDisplay(toAxisX(0)).doubleValue()
                        - xAxis.getValueForDisplay(toAxisX(dx)).doubleValue();

                double yShift = yAxis.getValueForDisplay(toAxisY(0)).doubleValue()
                        - yAxis.getValueForDisplay(toAxisY(dy)).doubleValue();

                xAxis.setLowerBound(xAxis.getLowerBound() + xShift);
                xAxis.setUpperBound(xAxis.getUpperBound() + xShift);
                yAxis.setLowerBound(yAxis.getLowerBound() + yShift);
                yAxis.setUpperBound(yAxis.getUpperBound() + yShift);
                clampAxesToData();
            } else {
                // ZOOM-RECT drag
                if (dragAnchor == null || !zoomRect.isVisible()) return;

                double ax = dragAnchor.getX();
                double ay = dragAnchor.getY();
                double x = e.getX();
                double y = e.getY();

                zoomRect.setX(Math.min(ax, x));
                zoomRect.setY(Math.min(ay, y));
                zoomRect.setWidth(Math.abs(x - ax));
                zoomRect.setHeight(Math.abs(y - ay));
            }

            e.consume();
        });

        // --- Mouse released: apply zoom rectangle if used ---
        chartContainer.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> {
            if (zoomRect.isVisible()) {
                zoomRect.setVisible(false);

                if (zoomRect.getWidth() >= 10 && zoomRect.getHeight() >= 10) {
                    xAxis.setAutoRanging(false);
                    yAxis.setAutoRanging(false);

                    double xMin = xAxis.getValueForDisplay(toAxisX(zoomRect.getX())).doubleValue();
                    double xMax = xAxis.getValueForDisplay(toAxisX(zoomRect.getX() + zoomRect.getWidth())).doubleValue();
                    double yMin = yAxis.getValueForDisplay(toAxisY(zoomRect.getY() + zoomRect.getHeight())).doubleValue();
                    double yMax = yAxis.getValueForDisplay(toAxisY(zoomRect.getY())).doubleValue();

                    xAxis.setLowerBound(Math.min(xMin, xMax));
                    xAxis.setUpperBound(Math.max(xMin, xMax));
                    yAxis.setLowerBound(Math.min(yMin, yMax));
                    yAxis.setUpperBound(Math.max(yMin, yMax));
                    clampAxesToData();
                }
            }

            dragAnchor = null;
            lastPan = null;
            e.consume();
        });

        // --- Reset: double-click OR right-click ---
        chartContainer.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() == 2 || e.isSecondaryButtonDown()) {
                xAxis.setAutoRanging(true);
                yAxis.setAutoRanging(true);
                e.consume();
            }
        });
    }

    private void initBoundsFromCurrentData() {
        if (lineChart.getData().isEmpty()) return;

        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (var s : lineChart.getData()) {
            for (var d : s.getData()) {
                if (d.getXValue() == null || d.getYValue() == null) continue;
                double x = d.getXValue().doubleValue();
                double y = d.getYValue().doubleValue();
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            }
        }
        if (!Double.isFinite(minX) || !Double.isFinite(minY)) return;

        double padX = (maxX - minX) * 0.05;
        double padY = (maxY - minY) * 0.10;

        xAxis.setLowerBound(minX - padX);
        xAxis.setUpperBound(maxX + padX);
        yAxis.setLowerBound(minY - padY);
        yAxis.setUpperBound(maxY + padY);
    }

    private void updateDataBoundsFromChart() {
        dataMinX = Double.POSITIVE_INFINITY; dataMaxX = Double.NEGATIVE_INFINITY;
        dataMinY = Double.POSITIVE_INFINITY; dataMaxY = Double.NEGATIVE_INFINITY;

        for (var s : lineChart.getData()) {
            for (var d : s.getData()) {
                if (d.getXValue() == null || d.getYValue() == null) continue;
                double x = d.getXValue().doubleValue();
                double y = d.getYValue().doubleValue();
                dataMinX = Math.min(dataMinX, x); dataMaxX = Math.max(dataMaxX, x);
                dataMinY = Math.min(dataMinY, y); dataMaxY = Math.max(dataMaxY, y);
            }
        }
        dataBoundsValid = Double.isFinite(dataMinX) && Double.isFinite(dataMinY) && dataMaxX > dataMinX;
    }

    private void zoomAxisAround(NumberAxis axis, double anchorValue, double factor) {
        double lower = axis.getLowerBound();
        double upper = axis.getUpperBound();
        double range = upper - lower;
        if (range <= 0) return;

        double newRange = range * factor;
        double anchorRatio = (anchorValue - lower) / range;

        double newLower = anchorValue - anchorRatio * newRange;
        double newUpper = newLower + newRange;

        axis.setLowerBound(newLower);
        axis.setUpperBound(newUpper);
    }

    private double toAxisX(double containerX) {
        javafx.geometry.Point2D p = xAxis.sceneToLocal(chartContainer.localToScene(containerX, 0));
        return p.getX();
    }

    private double toAxisY(double containerY) {
        javafx.geometry.Point2D p = yAxis.sceneToLocal(chartContainer.localToScene(0, containerY));
        return p.getY();
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
            tc.granularity = SensorReadingStatisticsRepository.Granularity.HOUR;
        } else if ("30d".equals(range)) {
            tc.from = tc.to.minus(Duration.ofDays(30));
            tc.granularity = SensorReadingStatisticsRepository.Granularity.HOUR;
        } else {
            tc.from = tc.to.minus(Duration.ofHours(24));
            tc.granularity = SensorReadingStatisticsRepository.Granularity.HOUR;
        }
        return tc;
    }

    private void fixLegendColors() {
        javafx.application.Platform.runLater(() -> {
            var legend = lineChart.lookup(".chart-legend");
            if (legend == null) return;

            legend.lookupAll(".chart-legend-item").forEach(item -> {
                var label = item.lookup(".label");
                var symbol = item.lookup(".chart-legend-item-symbol");

                if (label == null || symbol == null) return;

                String text = ((javafx.scene.control.Label) label).getText();

                if (text.endsWith("(Avg)")) {
                    symbol.setStyle("-fx-background-color: #f59e0b, white;");
                } else if (text.endsWith("(Min)")) {
                    symbol.setStyle("-fx-background-color: #ef4444, white;");
                } else if (text.endsWith("(Max)")) {
                    symbol.setStyle("-fx-background-color: #22c55e, white;");
                }
            });
        });
    }

    private boolean isDashboardEmpty() {
        return deviceRepo.getSensorDevicesByHomeId(home.getId()).isEmpty()
                || !readingRepo.hasAnyReadingsForHome(home.getId());
    }

    private void updateSeedControlsVisibility() {
        boolean show = isDashboardEmpty();

        seedButton.setVisible(show);
        seedButton.setManaged(show);

        seedRangeBox.setVisible(show);
        seedRangeBox.setManaged(show);

        seedCard.setVisible(true);
        seedCard.setManaged(true);

        if (seedHintLabel != null) {
            seedHintLabel.setVisible(show);
            seedHintLabel.setManaged(show);
            seedHintLabel.setText("No sensor data yet. Generate demo readings:");
        }
    }

    private void clampAxesToData() {
        if (!dataBoundsValid) return;

        double padX = (dataMaxX - dataMinX) * 0.02;
        double padY = (dataMaxY - dataMinY) * 0.10;

        double minX = dataMinX - padX, maxX = dataMaxX + padX;
        double minY = dataMinY - padY, maxY = dataMaxY + padY;

        // Clamp X
        double lx = xAxis.getLowerBound(), ux = xAxis.getUpperBound();
        double wX = ux - lx;

        if (wX > (maxX - minX)) { // don't zoom out beyond full data span
            xAxis.setLowerBound(minX);
            xAxis.setUpperBound(maxX);
        } else {
            if (lx < minX) { xAxis.setLowerBound(minX); xAxis.setUpperBound(minX + wX); }
            if (ux > maxX) { xAxis.setUpperBound(maxX); xAxis.setLowerBound(maxX - wX); }
        }

        // Clamp Y (optional; usually OK to clamp too)
        double ly = yAxis.getLowerBound(), uy = yAxis.getUpperBound();
        double wY = uy - ly;

        if (wY > (maxY - minY)) {
            yAxis.setLowerBound(minY);
            yAxis.setUpperBound(maxY);
        } else {
            if (ly < minY) { yAxis.setLowerBound(minY); yAxis.setUpperBound(minY + wY); }
            if (uy > maxY) { yAxis.setUpperBound(maxY); yAxis.setLowerBound(maxY - wY); }
        }
    }

    /**
     * Resets the chart view to the bounds of the currently loaded data.
     *
     * <p>Any previous zoom or pan state is cleared by temporarily enabling
     * auto-ranging on both axes. After the JavaFX layout pass completes,
     * auto-ranging is disabled again and the axis bounds are recalculated
     * based on the current data.</p>
     *
     * <p>The deferred execution via {@code Platform.runLater} ensures that
     * chart nodes and layout are fully initialized before bounds are locked.</p>
     */
    private void resetViewToData() {
        // Reset any previous zoom/pan state
        xAxis.setAutoRanging(true);
        yAxis.setAutoRanging(true);

        javafx.application.Platform.runLater(() -> {
            // Make sure nodes + layout exist
            lineChart.applyCss();
            lineChart.layout();

            // Lock bounds and pad to data
            xAxis.setAutoRanging(false);
            yAxis.setAutoRanging(false);
            initBoundsFromCurrentData();

            // Keep within data bounds if you want
            updateDataBoundsFromChart();
            clampAxesToData();
        });
    }

    private void applyTickUnit(TimeConfig tc) {
        if ("30d".equals(rangeBox.getValue())) {
            xAxis.setTickUnit(Duration.ofDays(2).toMillis()); // label every 2 days
        } else if ("7d".equals(rangeBox.getValue())) {
            xAxis.setTickUnit(Duration.ofHours(12).toMillis());
        } else {
            xAxis.setTickUnit(Duration.ofHours(2).toMillis());
        }
    }
}
