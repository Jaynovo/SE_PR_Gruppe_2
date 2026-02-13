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

/**
 * Controller for the statistics dashboard view displaying sensor data charts and KPIs.
 *
 * <p>This controller handles {@code statistics_dashboard_page.fxml} and provides
 * interactive data visualization for sensor readings across the home. It includes:</p>
 * <ul>
 *   <li>A line chart displaying Min, Avg, and Max sensor values over time</li>
 *   <li>KPI labels showing average, minimum, maximum, and count of readings</li>
 *   <li>Scope filtering by Home or individual Room</li>
 *   <li>Metric selection from available sensor types</li>
 *   <li>Time range selection (24h, 7d, 30d)</li>
 *   <li>Interactive chart interactions (scroll zoom, drag-to-zoom, shift-to-pan)</li>
 *   <li>Demo data seeding when no sensor data exists yet</li>
 * </ul>
 *
 * <p><b>Chart interactions:</b></p>
 * <ul>
 *   <li>Scroll wheel: zoom in/out on X axis around the cursor</li>
 *   <li>Click and drag: draw a zoom rectangle to zoom into a region</li>
 *   <li>Shift + drag: pan the chart view</li>
 *   <li>Double-click or right-click: reset to auto-ranging</li>
 * </ul>
 *
 * <p><b>Demo data:</b> If no sensor readings exist for the home, a "seed data"
 * UI is shown to allow generating demo readings for development/testing purposes.</p>
 *
 * @see StatisticsService
 * @see DemoDataSeeder
 * @see SensorReadingStatisticsRepository
 */
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

    /**
     * Initializes the statistics dashboard for the current user and home.
     *
     * <p>Initialization steps:</p>
     * <ol>
     *   <li>Loads the current user and their home from the session</li>
     *   <li>Disables the dashboard with a message if no user or home is found</li>
     *   <li>Populates the room, metric, time range, and seed interval combo boxes</li>
     *   <li>Configures room cell factories for display</li>
     *   <li>Sets the default scope based on session context (room-specific vs. home-wide)</li>
     *   <li>Attaches change listeners to re-trigger {@link #refresh()} on selection change</li>
     *   <li>Shows/hides seed controls based on whether data exists</li>
     *   <li>Installs chart interaction handlers and performs initial refresh</li>
     * </ol>
     *
     * @param location the URL used to resolve relative paths (unused)
     * @param resources the resources for localization (unused)
     */
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

    /**
     * Handles the Refresh button action.
     *
     * <p>Delegates to the internal {@link #refresh()} method to re-fetch and
     * redraw all KPIs and chart data.</p>
     */
    @FXML
    public void handleRefresh() {
        refresh();
    }

    /**
     * Handles the Seed button action.
     *
     * <p>Seeds missing demo data for the current home using the selected interval
     * preset, then refreshes the dashboard and hides the seed controls.</p>
     */
    private void handleSeed() {
        var preset = seedRangeBox.getValue();
        if (preset == null) preset = DemoDataSeeder.IntervalPreset.LAST_7D;

        demoSeeder.seedIfMissing(home.getId(), preset.duration);

        refresh();                    // rerender KPIs + chart
        updateSeedControlsVisibility(); // hide CTA afterwards
    }

    /**
     * Fetches KPIs and chart data for the current scope/metric/range selection
     * and updates the UI.
     *
     * <p>If the dashboard has no data, clears all labels and returns early.
     * Otherwise:</p>
     * <ol>
     *   <li>Resolves the current scope (home or room)</li>
     *   <li>Computes the time range and granularity</li>
     *   <li>Fetches KPI statistics (avg, min, max, count)</li>
     *   <li>Fetches Min, Avg, Max time series</li>
     *   <li>Renders all three series on the line chart</li>
     * </ol>
     */
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

    /**
     * Resolves the current dashboard scope from the scope combo box selection.
     *
     * @return a {@link StatisticsService.DashboardScope} for the home or selected room,
     *         or {@code null} if the scope is "Room" but no room is selected
     */
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

    /**
     * Renders Min, Avg, and Max sensor data series onto the line chart.
     *
     * <p>Clears existing chart data, builds three named series, replaces them
     * in the chart, then applies CSS classes, tooltips, resets the view, and
     * fixes legend colors.</p>
     *
     * @param metricLabel the label of the selected metric, used as series name prefix
     * @param minPoints data points for the minimum aggregation series
     * @param avgPoints data points for the average aggregation series
     * @param maxPoints data points for the maximum aggregation series
     */
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

    /**
     * Builds a named {@link XYChart.Series} from a list of time-bucketed data points.
     *
     * <p>Null buckets or null values are skipped. X values are epoch milliseconds
     * (suitable for the {@link NumberAxis} with a custom tick label formatter).</p>
     *
     * @param name the series name shown in the legend
     * @param points the data points to plot (must not be {@code null})
     * @return a populated series (never {@code null}, may be empty if all points are null)
     */
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

    /**
     * Applies a CSS class to a chart series' line node.
     *
     * <p>Used to apply colored series lines ("series-min", "series-avg", "series-max")
     * defined in the application stylesheet. The class is not added if already present.</p>
     *
     * @param series the series whose node to style
     * @param cssClass the CSS class to apply
     */
    private void applySeriesClass(XYChart.Series<Number, Number> series, String cssClass) {
        var node = series.getNode();
        if (node != null && !node.getStyleClass().contains(cssClass)) {
            node.getStyleClass().add(cssClass);
        }
    }

    /**
     * Configures the X-axis tick label formatter to display human-readable timestamps.
     *
     * <p>Converts epoch millisecond values on the X-axis to "MM-dd HH:mm" format
     * in the system default timezone.</p>
     */
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

    /**
     * Installs hover tooltips on data points of the given series.
     *
     * <p>Each data point receives a {@link Tooltip} showing timestamp and value.
     * Points also scale up on hover for visual feedback.</p>
     *
     * @param series the series whose data points to annotate with tooltips
     */
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

    /**
     * The zoom selection rectangle overlay, drawn on the chart container during drag-to-zoom.
     */
    private Rectangle zoomRect;

    /**
     * The start point (in container coordinates) of the current zoom rectangle drag.
     */
    private Point2D dragAnchor;

    /**
     * The last mouse position (in container coordinates) during a pan operation.
     */
    private Point2D lastPan;

    /**
     * Installs all interactive chart behaviors (zoom, pan, reset) on the chart container.
     *
     * <p><b>Installed interactions:</b></p>
     * <ul>
     *   <li><b>Scroll:</b> Zooms in/out on the X axis around the cursor position</li>
     *   <li><b>Primary drag (no modifier):</b> Draws a zoom rectangle; applying zoom on release</li>
     *   <li><b>Primary drag + Shift:</b> Pans the chart view</li>
     *   <li><b>Double-click or right-click:</b> Resets axes to auto-ranging</li>
     * </ul>
     *
     * <p>A zoom rectangle overlay ({@link Rectangle}) is created and added to the
     * chart container on first call. It is styled via the "zoom-rect" CSS class.</p>
     */
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
            double factor = (e.getDeltaY() > 0) ? 0.9 : 1.1;

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

    /**
     * Initializes axis bounds from the current chart data with padding.
     *
     * <p>Scans all series in the chart to find the min/max X and Y values,
     * then sets axis bounds with 5% X padding and 10% Y padding.
     * Does nothing if the chart is empty or data is not finite.</p>
     */
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

    /**
     * Rescans the current chart data to update {@code dataMinX/Y} and {@code dataMaxX/Y}.
     *
     * <p>These cached bounds are used by {@link #clampAxesToData()} to prevent
     * zooming or panning beyond the extent of the loaded data.</p>
     */
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

    /**
     * Zooms an axis in or out around a specific data value.
     *
     * <p>The {@code anchorValue} remains stationary while the visible range
     * is scaled by {@code factor}. A factor less than 1.0 zooms in; greater
     * than 1.0 zooms out.</p>
     *
     * @param axis the axis to zoom (must not be {@code null})
     * @param anchorValue the data value to zoom around (anchor point)
     * @param factor the zoom scale factor (positive; {@literal <}1.0 = zoom in, {@literal >}1.0 = zoom out)
     */
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

    /**
     * Converts a container X coordinate to axis display units for the X axis.
     *
     * @param containerX the X coordinate in chart container local space
     * @return the corresponding X coordinate in axis display space
     */
    private double toAxisX(double containerX) {
        javafx.geometry.Point2D p = xAxis.sceneToLocal(chartContainer.localToScene(containerX, 0));
        return p.getX();
    }

    /**
     * Converts a container Y coordinate to axis display units for the Y axis.
     *
     * @param containerY the Y coordinate in chart container local space
     * @return the corresponding Y coordinate in axis display space
     */
    private double toAxisY(double containerY) {
        javafx.geometry.Point2D p = yAxis.sceneToLocal(chartContainer.localToScene(0, containerY));
        return p.getY();
    }

    /**
     * Formats a nullable {@link Double} value to two decimal places (US locale).
     *
     * @param v the value to format (may be {@code null})
     * @return formatted string, or "-" if {@code v} is {@code null}
     */
    private static String format(Double v) {
        if (v == null) return "-";
        return String.format(Locale.US, "%.2f", v);
    }

    /**
     * Disables all dashboard controls and shows a message in the average label.
     *
     * <p>Called when initialization fails (no user logged in or no home available).</p>
     *
     * @param msg the message to display in the average label (must not be {@code null})
     */
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

    /**
     * Clears all KPI labels and removes chart data.
     *
     * <p>Called when the dashboard is empty (no data) or when a selection
     * change results in no valid scope.</p>
     */
    private void clearStats() {
        avgLabel.setText("-");
        minLabel.setText("-");
        maxLabel.setText("-");
        countLabel.setText("-");
        lineChart.getData().clear();
    }

    /**
     * Configuration record for a time range query.
     *
     * <p>Holds the start/end instants and the granularity (bucket size) for
     * fetching time-bucketed sensor statistics.</p>
     */
    private static class TimeConfig {
        Instant from;
        Instant to;
        SensorReadingStatisticsRepository.Granularity granularity;
    }

    /**
     * Computes the {@link TimeConfig} for the given time range string.
     *
     * <p>Mapping:</p>
     * <ul>
     *   <li>"7d" → last 7 days, hourly granularity</li>
     *   <li>"30d" → last 30 days, hourly granularity</li>
     *   <li>Default ("24h") → last 24 hours, hourly granularity</li>
     * </ul>
     *
     * @param range the range string (e.g., "24h", "7d", "30d")
     * @return a fully populated {@link TimeConfig} with {@code from}, {@code to}, and {@code granularity}
     */
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

    /**
     * Fixes legend item symbol colors to match their corresponding series CSS colors.
     *
     * <p>JavaFX's default legend coloring may not match CSS-applied series colors,
     * so this method manually overrides symbol styles in the legend for Min, Avg,
     * and Max series.</p>
     *
     * <p>Executed on the JavaFX application thread via {@code Platform.runLater}.</p>
     */
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

    /**
     * Returns whether the dashboard has no sensor data to display.
     *
     * <p>Returns {@code true} if either:</p>
     * <ul>
     *   <li>No sensor devices exist for the home</li>
     *   <li>No sensor readings have been recorded for the home</li>
     * </ul>
     *
     * @return {@code true} if no data is available, {@code false} otherwise
     */
    private boolean isDashboardEmpty() {
        return deviceRepo.getSensorDevicesByHomeId(home.getId()).isEmpty()
                || !readingRepo.hasAnyReadingsForHome(home.getId());
    }

    /**
     * Updates the visibility of the seed data controls based on data availability.
     *
     * <p>Seed controls (button, range box, hint label) are shown only when
     * {@link #isDashboardEmpty()} returns {@code true}. The seed card container
     * is always shown to avoid layout shifts.</p>
     */
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

    /**
     * Clamps the current axis bounds to the data extent, preventing over-panning or over-zooming.
     *
     * <p>If the current axis window is wider than the data span, it is reset to
     * the full data span. Otherwise, it is shifted to stay within bounds.</p>
     *
     * <p>Does nothing if {@link #dataBoundsValid} is {@code false}.</p>
     */
    private void clampAxesToData() {
        if (!dataBoundsValid) return;

        double padX = (dataMaxX - dataMinX) * 0.02;
        double padY = (dataMaxY - dataMinY) * 0.10;

        double minX = dataMinX - padX, maxX = dataMaxX + padX;
        double minY = dataMinY - padY, maxY = dataMaxY + padY;

        // Clamp X
        double lx = xAxis.getLowerBound(), ux = xAxis.getUpperBound();
        double wX = ux - lx;

        if (wX > (maxX - minX)) {
            xAxis.setLowerBound(minX);
            xAxis.setUpperBound(maxX);
        } else {
            if (lx < minX) { xAxis.setLowerBound(minX); xAxis.setUpperBound(minX + wX); }
            if (ux > maxX) { xAxis.setUpperBound(maxX); xAxis.setLowerBound(maxX - wX); }
        }

        // Clamp Y
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
        xAxis.setAutoRanging(true);
        yAxis.setAutoRanging(true);

        javafx.application.Platform.runLater(() -> {
            lineChart.applyCss();
            lineChart.layout();

            xAxis.setAutoRanging(false);
            yAxis.setAutoRanging(false);
            initBoundsFromCurrentData();

            updateDataBoundsFromChart();
            clampAxesToData();
        });
    }

    /**
     * Sets the X-axis tick unit based on the selected time range.
     *
     * <p>Tick spacing:</p>
     * <ul>
     *   <li>"30d" → one tick every 2 days</li>
     *   <li>"7d" → one tick every 12 hours</li>
     *   <li>"24h" (default) → one tick every 2 hours</li>
     * </ul>
     *
     * @param tc the time configuration (used to determine which range is active)
     */
    private void applyTickUnit(TimeConfig tc) {
        if ("30d".equals(rangeBox.getValue())) {
            xAxis.setTickUnit(Duration.ofDays(2).toMillis());
        } else if ("7d".equals(rangeBox.getValue())) {
            xAxis.setTickUnit(Duration.ofHours(12).toMillis());
        } else {
            xAxis.setTickUnit(Duration.ofHours(2).toMillis());
        }
    }
}