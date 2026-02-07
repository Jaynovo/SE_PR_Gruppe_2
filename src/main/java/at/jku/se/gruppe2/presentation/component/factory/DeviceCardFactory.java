package at.jku.se.gruppe2.presentation.component.factory;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.config.HeatingConfig;
import at.jku.se.gruppe2.domain.model.device.sensor.CatSensor;
import at.jku.se.gruppe2.domain.model.device.sensor.Sensor;
import at.jku.se.gruppe2.domain.model.device.sensor.Thermometer;
import at.jku.se.gruppe2.domain.service.device.ActuatorConfigService;
import at.jku.se.gruppe2.domain.service.device.ActuatorService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceCardFactory {
    private final ActuatorService actuatorService;
    private final ActuatorConfigService actuatorCfg;
    private final java.util.function.Consumer<Device> onDelete;
    private final java.util.function.Consumer<Device> onConfigure;
    private final java.util.function.Consumer<Device> onEdit;
    private final Runnable requestRender;

    // Cat sensor image tracking
    private final Map<Integer, ImageView> catImageViews = new HashMap<>();
    private final Map<Integer, Label> catStatusLabels = new HashMap<>();
    private final Map<Integer, String> catLastShownUrl = new HashMap<>();

    //Temp and heater
    private final Map<Integer, Label> sensorValueLabels = new HashMap<>();
    private final Map<Integer, Label> heaterPercentBadges = new HashMap<>();
    private final Map<Integer, Slider> heaterSliders = new HashMap<>();

    //Vent for humidity
    private final Map<Integer, ToggleButton> actuatorToggles = new HashMap<>();

    public DeviceCardFactory(
            ActuatorConfigService actuatorConfigService,
            ActuatorService actuatorService,
            java.util.function.Consumer<Device> onDelete,
            java.util.function.Consumer<Device> onConfigure,
            java.util.function.Consumer<Device> onEdit,
            Runnable requestRender) {
        this.actuatorService = actuatorService;
        this.actuatorCfg = actuatorConfigService;
        this.onDelete = onDelete;
        this.onConfigure = onConfigure;
        this.onEdit = onEdit;
        this.requestRender = requestRender;
    }

    /**
     * Creates a device card with appropriate UI elements based on device type
     *
     * @param device       The device to create a card for
     * @param canDelete    Whether the user can delete this device
     * @param canConfigure Whether the user can configure this device
     * @return A Pane containing the device card UI
     */
    public Pane createDeviceCard(Device device, boolean canDelete, boolean canConfigure) {
        VBox deviceCard = new VBox(8);
        deviceCard.getStyleClass().add("card");
        deviceCard.setPrefWidth(260);
        deviceCard.setPadding(new Insets(10));

        // Create top bar with title and edit button
        HBox topBar = createTopBar(device);

        Label type = new Label("Type: " + device.getTypeLabel());
        type.getStyleClass().add("muted");

        deviceCard.getChildren().addAll(topBar, type);

        // Handle sensors
        if (device instanceof Sensor s) {
            addSensorContent(deviceCard, device, s);
        }

        // Handle actuators
        if (device.getCategory() == Device.DeviceCategory.ACTUATOR) {
            return createActuatorCard(deviceCard, device, canDelete, canConfigure);
        }

        // Add delete button for non-actuator devices
        HBox actions = new HBox(8);
        if (canDelete) {
            Button deleteBtn = new Button("Delete");
            deleteBtn.setOnAction(e -> onDelete.accept(device));
            actions.getChildren().add(deleteBtn);
        }
        deviceCard.getChildren().add(actions);

        return deviceCard;
    }

    /**
     * Creates the top bar with device name and edit button
     */
    private HBox createTopBar(Device device) {
        Label title = new Label(device.getLabel());
        title.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(title, spacer);

        // Add edit button
        if (onEdit != null) {
            Button editButton = new Button("✏");
            editButton.getStyleClass().addAll("icon-button", "edit-button");
            editButton.setOnAction(e -> onEdit.accept(device));
            editButton.setStyle("-fx-font-size: 14px; -fx-padding: 2 6 2 6; -fx-cursor: hand;");
            topBar.getChildren().add(editButton);
        }

        return topBar;
    }

    /**
     * Adds sensor-specific content to the device card
     */
    private void addSensorContent(VBox deviceCard, Device device, Sensor s) {
        if ("CatSensor".equalsIgnoreCase(device.getTypeLabel()) && s instanceof CatSensor cat) {
            addCatSensorContent(deviceCard, device, cat);
        } else {
            addStandardSensorContent(deviceCard, device, s);
        }
    }

    /**
     * Adds cat sensor specific UI elements (image and status)
     */
    private void addCatSensorContent(VBox deviceCard, Device device, CatSensor cat) {
        // Status Label (created once and reused)
        Label statusLbl = catStatusLabels.computeIfAbsent(device.getId(), id -> {
            Label l = new Label();
            l.getStyleClass().add("badge");
            return l;
        });

        // ImageView (created once and reused)
        ImageView imgView = catImageViews.computeIfAbsent(device.getId(), id -> {
            ImageView iv = new ImageView();
            iv.setFitWidth(180);
            iv.setFitHeight(120);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            return iv;
        });

        updateCatCardUI(device.getId(), cat, statusLbl, imgView);

        StackPane imageCard = new StackPane(imgView);
        imageCard.getStyleClass().add("mini-image-card");
        imageCard.setPrefSize(180, 120);

        deviceCard.getChildren().addAll(statusLbl, imageCard);
    }

    /**
     * Adds standard sensor content (value display)
     */
    private void addStandardSensorContent(VBox deviceCard, Device device, Sensor s) {
        String unit = resolveDisplayUnit(device);
        unit = (unit == null) ? "Not available!" : unit;

        double displayValue = s.getValue();
        if (s instanceof Thermometer t) {
            displayValue = t.getValueInSelectedUnit();
        }

        String formattedValue = formatSensorValue(device.getTypeLabel(), displayValue);

        Label current = new Label();
        current.getStyleClass().add("muted");
        sensorValueLabels.put(device.getId(), current);

        updateStandardSensorLabel(device, s, current);  // initial setzen
        deviceCard.getChildren().add(current);

        if (device instanceof Thermometer t) {
            deviceCard.getChildren().add(createThermometerUnitToggle(t));
        }
    }

    private void updateStandardSensorLabel(Device device, Sensor s, Label current) {
        String unit = resolveDisplayUnit(device);
        unit = (unit == null) ? "" : unit;

        double displayValue = s.getValue();
        if (s instanceof Thermometer t) {
            displayValue = t.getValueInSelectedUnit();
        }

        String formattedValue = formatSensorValue(device.getTypeLabel(), displayValue);
        current.setText("Current: " + formattedValue + (unit.isBlank() ? "" : " " + unit));
    }

    /**
     * Creates an actuator card with appropriate controls
     */
    private Pane createActuatorCard(VBox deviceCard, Device device, boolean canDelete, boolean canConfigure) {
        String typeLabel = device.getTypeLabel();

        // AlarmSystem: DISARMED / ARMED / TRIGGERED
        if ("AlarmSystem".equalsIgnoreCase(typeLabel)) {
            return createAlarmSystemCard(deviceCard, device, canDelete, canConfigure);
        }

        // Cat Feeder
        if ("Cat Feeder".equalsIgnoreCase(typeLabel)) {
            return createCatFeederCard(deviceCard, device, canDelete, canConfigure);
        }
        if ("Heating".equalsIgnoreCase(typeLabel) || typeLabel.toLowerCase().contains("heating")) {
            return createHeatingCard(deviceCard, device, canDelete, canConfigure);
        }

        // Standard Actuator: ON / OFF
        return createStandardActuatorCard(deviceCard, device, canDelete, canConfigure);
    }

    private Pane createHeatingCard(VBox deviceCard, Device device, boolean canDelete, boolean canConfigure) {

        // --- current state (0..100) ---
        int percent = safeParsePercent(actuatorService.getStateOrDefault(device.getId(), "0"));

        // --- badge (store ref for live updates) ---
        Label badge = new Label(percent + " %");
        badge.getStyleClass().addAll("badge", "badge-off");
        heaterPercentBadges.put(device.getId(), badge);

        // --- slider (store ref for live updates) ---
        Slider slider = new Slider(0, 100, percent);
        slider.setMajorTickUnit(25);
        slider.setMinorTickCount(4);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setSnapToTicks(true);
        heaterSliders.put(device.getId(), slider);

        // slider -> badge live (only visual; no state write yet)
        slider.valueProperty().addListener((obs, oldV, newV) -> {
            int p = (int) Math.round(newV.doubleValue());
            badge.setText(p + " %");
        });

        // --- Apply: writes MANUAL percent into HeatingConfig + sets actuator state ---
        Button applyBtn = new Button("Apply");
        applyBtn.setDisable(!canConfigure); // only if allowed to control/configure
        applyBtn.setOnAction(e -> {
            int p = (int) Math.round(slider.getValue());

            // 1) save to config as MANUAL (auto off)
            HeatingConfig cfg = actuatorCfg.getOrCreateHeatingConfig(device.getId());
            cfg.setAutoMode(false);
            cfg.setManualPercent(p);
            actuatorCfg.saveHeatingConfig(device.getId(), cfg);

            // 2) set actuator state immediately (UI shows new %)
            actuatorService.setState(device.getId(), String.valueOf(p));

            // IMPORTANT: do NOT call requestRender.run() here -> causes flicker.
            // Live-refresh will call updateLiveValues() and keep UI in sync.
        });

        // --- Configure / Delete ---
        Button configBtn = new Button("Config");
        configBtn.setVisible(canConfigure);
        configBtn.setManaged(canConfigure);
        configBtn.setOnAction(e -> onConfigure.accept(device));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setVisible(canDelete);
        deleteBtn.setManaged(canDelete);
        deleteBtn.setOnAction(e -> onDelete.accept(device));

        // --- layout ---
        HBox rowTop = new HBox(8, badge);
        HBox rowBottom = new HBox(8);

        // nicer sizing so it doesn't truncate
        applyBtn.setPrefWidth(80);
        configBtn.setPrefWidth(80);
        deleteBtn.setPrefWidth(80);

        rowBottom.getChildren().add(applyBtn);
        if (canConfigure) rowBottom.getChildren().add(configBtn);
        if (canDelete) rowBottom.getChildren().add(deleteBtn);

        deviceCard.getChildren().addAll(rowTop, slider, rowBottom);
        return deviceCard;
    }

    /**
     * Creates an alarm system actuator card
     */
    private Pane createAlarmSystemCard(VBox deviceCard, Device device, boolean canDelete, boolean canConfigure) {
        String currentState = actuatorService.getStateOrDefault(device.getId(), "DISARMED");
        boolean isArmed = "ARMED".equalsIgnoreCase(currentState);
        boolean isTriggered = "TRIGGERED".equalsIgnoreCase(currentState);

        ToggleButton armToggle = new ToggleButton();
        armToggle.getStyleClass().add("actuator-toggle");

        // If TRIGGERED: disable toggle (user should press Reset)
        armToggle.setDisable(isTriggered);

        // selected => ARMED, unselected => DISARMED
        armToggle.setSelected(isArmed);
        armToggle.setText(isTriggered ? "TRIGGERED" : (isArmed ? "ARMED" : "DISARMED"));

        armToggle.selectedProperty().addListener((obs, oldVal, on) -> {
            String newState = on ? "ARMED" : "DISARMED";
            armToggle.setText(newState);
            actuatorService.setState(device.getId(), newState);

            // Reset debounce counter
            actuatorCfg.resetAlarmNoiseCounter(device.getId());
        });

        Button resetBtn = new Button("Reset");
        resetBtn.setOnAction(e -> {
            actuatorService.setState(device.getId(), "DISARMED");
            actuatorCfg.resetAlarmNoiseCounter(device.getId());
            requestRender.run(); // Redraw UI
        });

        // CONFIGURE button only shows if user has permission
        Button configBtn = new Button("Configure");
        configBtn.setVisible(canConfigure);
        configBtn.setManaged(canConfigure);
        configBtn.setOnAction(e -> onConfigure.accept(device));

        VBox actionBox = new VBox(6);
        HBox row1 = new HBox(8);
        HBox row2 = new HBox(8);

        // Row 1: Status/Arm + Reset
        row1.getChildren().addAll(armToggle, resetBtn);

        // Row 2: Configure + Delete (only if permissions allow)
        if (canConfigure || canDelete) {
            if (canConfigure) row2.getChildren().add(configBtn);
            if (canDelete) {
                Button deleteBtn = new Button("Delete");
                deleteBtn.setOnAction(e -> onDelete.accept(device));
                row2.getChildren().add(deleteBtn);
            }
            actionBox.getChildren().addAll(row1, row2);
        } else {
            actionBox.getChildren().add(row1);
        }

        deviceCard.getChildren().add(actionBox);
        return deviceCard;
    }

    /**
     * Creates a cat feeder actuator card
     */
    private Pane createCatFeederCard(VBox deviceCard, Device device, boolean canDelete, boolean canConfigure) {
        // Get status from service
        String feederState = actuatorService.getStateOrDefault(device.getId(), "READY");
        int cd = actuatorCfg.getCatFeederCooldown(device.getId()); // ticks
        String stateText = feederState;

        // Show cooldown
        if ("COOLDOWN".equalsIgnoreCase(feederState) && cd > 0) {
            stateText = "COOLDOWN (" + (cd * 2) + "s)";
        }

        // Status button (display only)
        Button statusBtn = new Button(stateText);
        statusBtn.setDisable(true);

        // Base class
        statusBtn.getStyleClass().add("feeder-status");

        // Remove old state classes (important for re-render!)
        statusBtn.getStyleClass().removeAll(
                "feeder-ready",
                "feeder-feeding",
                "feeder-cooldown"
        );

        // Set state-specific class
        if (feederState.equalsIgnoreCase("READY")) {
            statusBtn.getStyleClass().add("feeder-ready");
        } else if (feederState.equalsIgnoreCase("FEEDING")) {
            statusBtn.getStyleClass().add("feeder-feeding");
        } else if (feederState.equalsIgnoreCase("COOLDOWN")) {
            statusBtn.getStyleClass().add("feeder-cooldown");
        } else {
            statusBtn.getStyleClass().add("feeder-cooldown");
        }
        statusBtn.getStyleClass().add("badge");

        // Color based on state
        statusBtn.getStyleClass().removeAll("badge-ok", "badge-warn", "badge-off");

        if (feederState.equalsIgnoreCase("READY")) {
            statusBtn.getStyleClass().add("badge-off");
        } else if (feederState.equalsIgnoreCase("FEEDING")) {
            statusBtn.getStyleClass().add("badge-warn");
        } else if (feederState.equalsIgnoreCase("COOLDOWN")) {
            statusBtn.getStyleClass().add("badge-off");
        } else {
            statusBtn.getStyleClass().add("badge-off");
        }

        Button configBtn = new Button("Configure");
        configBtn.setVisible(canConfigure);
        configBtn.setManaged(canConfigure);
        configBtn.setOnAction(e -> onConfigure.accept(device));

        // Layout: Status on top, Configure/Delete below
        VBox actionBox = new VBox(8);
        HBox row = new HBox(8);

        if (canConfigure) {
            row.getChildren().add(configBtn);
        }

        if (canDelete) {
            Button deleteBtn = new Button("Delete");
            deleteBtn.setOnAction(e -> onDelete.accept(device));
            row.getChildren().add(deleteBtn);
        }

        actionBox.getChildren().addAll(statusBtn, row);
        deviceCard.getChildren().add(actionBox);
        return deviceCard;
    }

    /**
     * Creates a standard actuator card (ON/OFF toggle)
     */
    private Pane createStandardActuatorCard(VBox deviceCard, Device device, boolean canDelete, boolean canConfigure) {
        ToggleButton toggle = new ToggleButton();
        toggle.getStyleClass().add("actuator-toggle");
        actuatorToggles.put(device.getId(), toggle);

        String currentState = actuatorService.getStateOrDefault(device.getId(), "OFF");
        boolean isOn = "ON".equalsIgnoreCase(currentState);

        toggle.setSelected(isOn);
        toggle.setText(isOn ? "ON" : "OFF");

        toggle.selectedProperty().addListener((obs, oldVal, on) -> {
            String newState = on ? "ON" : "OFF";
            toggle.setText(newState);
            actuatorService.setState(device.getId(), newState);
        });

        HBox actions = new HBox(8);
        actions.getChildren().add(toggle);

        // CONFIGURE button only shows if user has permission
        if (canConfigure) {
            Button configBtn = new Button("Configure");
            configBtn.setOnAction(e -> onConfigure.accept(device));
            actions.getChildren().add(configBtn);
        }

        // DELETE button
        if (canDelete) {
            Button deleteBtn = new Button("Delete");
            deleteBtn.setOnAction(e -> onDelete.accept(device));
            actions.getChildren().add(deleteBtn);
        }

        deviceCard.getChildren().add(actions);
        return deviceCard;
    }

    /**
     * Updates the cat sensor card UI with current image and detection status
     */
    private void updateCatCardUI(int deviceId, CatSensor cat, Label statusLbl, ImageView imgView) {
        double conf = cat.getConfidence(); // 0..1
        String status = cat.isCatDetected() ? "CAT DETECTED" : "No cat detected";
        statusLbl.setText(status + " (" + Math.round(conf * 100) + "%)");

        String url = cat.getLastImageUrl();
        if (url == null || url.isBlank()) return;

        // Only update image if URL has changed (=> every 10s)
        String lastUrl = catLastShownUrl.get(deviceId);
        if (url.equals(lastUrl)) return;

        catLastShownUrl.put(deviceId, url);

        // backgroundLoading=true to prevent stuttering
        Image img = new Image(url, 0, 0, true, true, true);
        setImageCover(imgView, img, 180, 120);

        statusLbl.getStyleClass().removeAll("badge-ok", "badge-warn", "badge-off");

        boolean detected = cat.isCatDetected();
        if (detected) {
            statusLbl.getStyleClass().add("badge-ok");
        } else {
            statusLbl.getStyleClass().add("badge-off");
        }
    }

    /**
     * Sets an image in cover mode (fills the ImageView while maintaining aspect ratio)
     */
    private void setImageCover(ImageView iv, Image img, double w, double h) {
        iv.setImage(img);
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(true);

        // Wait for image to load, then set viewport crop
        if (img.getProgress() < 1.0) {
            img.progressProperty().addListener((obs, oldV, newV) -> {
                if (newV.doubleValue() >= 1.0) {
                    applyCoverViewport(iv, w, h);
                }
            });
        } else {
            applyCoverViewport(iv, w, h);
        }
    }

    /**
     * Applies cover-style viewport cropping to an ImageView
     */
    private void applyCoverViewport(ImageView iv, double w, double h) {
        Image img = iv.getImage();
        if (img == null) return;

        double imgW = img.getWidth();
        double imgH = img.getHeight();
        if (imgW <= 0 || imgH <= 0) return;

        double targetRatio = w / h;
        double imgRatio = imgW / imgH;

        double cropW, cropH;
        if (imgRatio > targetRatio) {
            // Too wide -> crop left/right
            cropH = imgH;
            cropW = imgH * targetRatio;
        } else {
            // Too tall -> crop top/bottom
            cropW = imgW;
            cropH = imgW / targetRatio;
        }

        double x = (imgW - cropW) / 2.0;
        double y = (imgH - cropH) / 2.0;

        iv.setViewport(new javafx.geometry.Rectangle2D(x, y, cropW, cropH));
    }

    /**
     * Formats sensor values based on sensor type
     */
    private String formatSensorValue(String typeLabel, double value) {
        if (typeLabel == null) return String.format("%.2f", value);

        if (typeLabel.equalsIgnoreCase("CO2Sensor")) {
            return String.format("%.0f", value);
        }
        // dB with 1 decimal place
        if (typeLabel.equalsIgnoreCase("NoiseSensor")) {
            return String.format("%.1f", value);
        }
        return String.format("%.2f", value);
    }

    /**
     * Resolves the display unit for a device (handles thermometer unit switching)
     */
    private String resolveDisplayUnit(Device device) {
        if (device instanceof Thermometer t) {
            return t.getDisplayUnit(); // °C or °F (instance-specific)
        }
        return device.getUnit(); // default from DeviceType
    }

    /**
     * Creates a temperature unit toggle (°C / °F) for thermometers
     */
    private Node createThermometerUnitToggle(Thermometer t) {
        ToggleGroup group = new ToggleGroup();

        RadioButton celsius = new RadioButton("°C");
        RadioButton fahrenheit = new RadioButton("°F");

        celsius.setToggleGroup(group);
        fahrenheit.setToggleGroup(group);

        if (t.getTemperatureUnit() == Thermometer.TemperatureUnit.CELSIUS) {
            celsius.setSelected(true);
        } else {
            fahrenheit.setSelected(true);
        }

        group.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == celsius) {
                t.setTemperatureUnit(Thermometer.TemperatureUnit.CELSIUS);
            } else if (newValue == fahrenheit) {
                t.setTemperatureUnit(Thermometer.TemperatureUnit.FAHRENHEIT);
            }
            requestRender.run(); // refreshes value display
        });

        HBox box = new HBox(8, new Label("Unit: "), celsius, fahrenheit);
        box.getStyleClass().add("muted");

        return box;
    }

    public void updateLiveValues(List<Device> devices) {
        for (Device d : devices) {

            // Sensor label updaten
            if (d instanceof Sensor s) {
                Label lbl = sensorValueLabels.get(d.getId());
                if (lbl != null) {
                    if ("CatSensor".equalsIgnoreCase(d.getTypeLabel()) && s instanceof CatSensor cat) {
                    } else {
                        updateStandardSensorLabel(d, s, lbl);
                    }
                }
            }

            // Heater percent badge + slider updaten
            if ("Heating".equalsIgnoreCase(d.getTypeLabel())) {
                String state = actuatorService.getStateOrDefault(d.getId(), "0");
                int pct = safeParsePercent(state);

                Label badge = heaterPercentBadges.get(d.getId());
                if (badge != null) badge.setText(pct + " %");

                Slider sl = heaterSliders.get(d.getId());
                if (sl != null && !sl.isValueChanging()) {
                    sl.setValue(pct);
                }
            }
            // Standard Actuator Toggle (z.B. Ventilation) updaten
            if (d.getCategory() == Device.DeviceCategory.ACTUATOR) {
                ToggleButton t = actuatorToggles.get(d.getId());
                if (t != null) {
                    String state = actuatorService.getStateOrDefault(d.getId(), "OFF");
                    boolean isOn = "ON".equalsIgnoreCase(state);


                    if (!t.isArmed() && !t.isPressed()) {
                        t.setSelected(isOn);
                        t.setText(isOn ? "ON" : "OFF");
                    }
                }
            }
        }
    }

    private int safeParsePercent(String s) {
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(s.trim())));
        } catch (Exception e) {
            return 0;
        }
    }
}