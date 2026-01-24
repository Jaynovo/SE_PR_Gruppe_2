package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.actuator.*;
import at.jku.se.gruppe2.model.sensor.*;
import at.jku.se.gruppe2.persistence.DeviceRepository;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.navigation.Page;
import at.jku.se.gruppe2.utils.Session;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class RoomDashboardController {

    @FXML
    public Label roomLabel;
    @FXML
    private FlowPane cardsFlow;

    private static final Duration REFRESH_INTERVAL = Duration.millis(500);

    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();
    private final DeviceRepository deviceRepository = new DeviceRepository();
    private final ActuatorService actuatorService = new ActuatorService();
    private final ActuatorConfigService actuatorCfg = new ActuatorConfigService();
    private final java.util.Map<Integer, javafx.scene.image.ImageView> catImageViews = new java.util.HashMap<>();
    private final java.util.Map<Integer, javafx.scene.control.Label> catStatusLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, String> catLastShownUrl = new java.util.HashMap<>();


    private final SensorSimulationService sensorSim = MainApp.getSensorSim();
    private Timeline liveRefresh;

    private List<Device> devices = new ArrayList<>();

    private String lastAlarmState = "DISARMED";

    public void initialize() {
        int userId = Session.getCurrentUser().getId();

        setLabel();
        loadDevicesAndRegisterSensors();
        renderDevices();

        //Alle 2 Sekunden neu rendern (gleich wie Simulation, alle 2 Sekunden neue Werte erzeugen)
        liveRefresh = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            evaluateAutomation();
            renderDevices();
        }));
        liveRefresh.setCycleCount(Timeline.INDEFINITE);
        liveRefresh.play();
    }

    private void setLabel() {
        String room = Session.getSelectedRoom().getRoomLabel();

        roomLabel.setText(room);
    }

    private void loadDevicesAndRegisterSensors() {
        Room room = Session.getSelectedRoom();
        devices = deviceRepository.getDevicesByRoomId(room.getId());

        sensorSim.clearRoom(room.getId());

        for (Device d : devices) {
            if (d instanceof Sensor s) {
                sensorSim.registerSensor(room.getId(), s);
            }
        }
    }

    private void renderDevices() {
        cardsFlow.getChildren().clear();
        for (Device device : devices) {
            cardsFlow.getChildren().add(createDeviceCard(device));
        }
    }

    private Pane createDeviceCard(Device device) {
        VBox deviceCard = new VBox(8);
        deviceCard.getStyleClass().add("card");
        deviceCard.setPrefWidth(260);
        deviceCard.setPadding(new Insets(10));

        Label title = new Label(device.getLabel());
        title.getStyleClass().add("card-title");

        Label type = new Label("Type: " + device.getTypeLabel());
        type.getStyleClass().add("muted");

        deviceCard.getChildren().addAll(title, type);
        if (device instanceof Sensor s) {
            if ("CatSensor".equalsIgnoreCase(device.getTypeLabel()) && s instanceof CatSensor cat) {

                // Status Label (einmalig)
                Label statusLbl = catStatusLabels.computeIfAbsent(device.getId(), id -> {
                    Label l = new Label();
                    l.getStyleClass().add("muted");
                    return l;
                });

                // ImageView (einmalig)
                javafx.scene.image.ImageView imgView = catImageViews.computeIfAbsent(device.getId(), id -> {
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
                    iv.setFitWidth(220);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    return iv;
                });

                // initialer Render (wird bei jedem renderDevices() aktualisiert, aber ohne Flackern)
                updateCatCardUI(device.getId(), cat, statusLbl, imgView);

                deviceCard.getChildren().addAll(statusLbl, imgView);
            } else {
                // bestehender Code für CO2/Noise/...
                String unit = resolveDisplayUnit(device);
                unit = (unit == null) ? "Not available!" : unit;

                String formattedValue = formatSensorValue(device.getTypeLabel(), s.getValue());
                Label current = new Label(
                        "Current: " + formattedValue + (unit.isBlank() ? "No measurement available!" : " " + unit)
                );
                current.getStyleClass().add("muted");

                deviceCard.getChildren().add(current);
                if (device instanceof Thermometer t) {
                    deviceCard.getChildren().add(createThermometerUnitToggle(t));
                }
            }
/*
            String unit = resolveDisplayUnit(device);
            unit = (unit == null) ? "Not available!" : unit;

            String formattedValue = formatSensorValue(device.getTypeLabel(), s.getValue());
            Label current = new Label(
                    "Current: " + formattedValue + (unit.isBlank() ? "No measurement available!" : " " + unit)
            );
            current.getStyleClass().add("muted");

            deviceCard.getChildren().add(current);
            if (device instanceof Thermometer t) {
                deviceCard.getChildren().add(createThermometerUnitToggle(t));
            } */
        }

        HBox actions = new HBox(8);

        //DELETE (immer)
        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> handleDeleteDevice(device));
        actions.getChildren().add(deleteBtn);

        //ACTUATOR
        if (device.getCategory() == Device.DeviceCategory.ACTUATOR) {

            String typeLabel = device.getTypeLabel();

            //AlarmSystem: DISARMED / ARMED / TRIGGERED
            if ("AlarmSystem".equalsIgnoreCase(typeLabel)) {

                String currentState = actuatorService.getStateOrDefault(device.getId(), "DISARMED");
                boolean isArmed = "ARMED".equalsIgnoreCase(currentState);
                boolean isTriggered = "TRIGGERED".equalsIgnoreCase(currentState);

                ToggleButton armToggle = new ToggleButton();
                armToggle.getStyleClass().add("actuator-toggle");

                // Wenn TRIGGERED: Toggle sperren (User soll Reset drücken)
                armToggle.setDisable(isTriggered);

                // selected => ARMED, unselected => DISARMED
                armToggle.setSelected(isArmed);
                armToggle.setText(isTriggered ? "TRIGGERED" : (isArmed ? "ARMED" : "DISARMED"));

                armToggle.selectedProperty().addListener((obs, oldVal, on) -> {
                    String newState = on ? "ARMED" : "DISARMED";
                    armToggle.setText(newState);
                    actuatorService.setState(device.getId(), newState);

                    // Debounce-Counter zurücksetzen
                    actuatorCfg.resetAlarmNoiseCounter(device.getId());
                });

                Button resetBtn = new Button("Reset");
                resetBtn.setOnAction(e -> {
                    actuatorService.setState(device.getId(), "DISARMED");
                    actuatorCfg.resetAlarmNoiseCounter(device.getId());
                    renderDevices(); // UI neu zeichnen
                });

                Button configBtn = new Button("Configure");
                configBtn.setOnAction(e -> handleConfigureActuator(device));

                VBox actionBox = new VBox(6);
                HBox row1 = new HBox(8);
                HBox row2 = new HBox(8);

                // Row 1: Status/Arm + Reset
                row1.getChildren().addAll(armToggle, resetBtn);

                // Row 2: Configure + Delete
                row2.getChildren().addAll(configBtn, deleteBtn);

                actionBox.getChildren().addAll(row1, row2);
                deviceCard.getChildren().add(actionBox);
                return deviceCard;

            } else {

                //Standard Aktor: ON / OFF
                ToggleButton toggle = new ToggleButton();
                toggle.getStyleClass().add("actuator-toggle");

                String currentState = actuatorService.getStateOrDefault(device.getId(), "OFF");
                boolean isOn = "ON".equalsIgnoreCase(currentState);

                toggle.setSelected(isOn);
                toggle.setText(isOn ? "ON" : "OFF");

                toggle.selectedProperty().addListener((obs, oldVal, on) -> {
                    String newState = on ? "ON" : "OFF";
                    toggle.setText(newState);
                    actuatorService.setState(device.getId(), newState);
                });

                Button configBtn = new Button("Configure");
                configBtn.setOnAction(e -> handleConfigureActuator(device));

                actions.getChildren().addAll(toggle, configBtn);
            }
        }

        deviceCard.getChildren().add(actions);
        return deviceCard;
    }
    private void updateCatCardUI(int deviceId, CatSensor cat, Label statusLbl, javafx.scene.image.ImageView imgView) {

        double conf = cat.getConfidence(); // 0..1
        String status = cat.isCatDetected() ? "CAT DETECTED" : "No cat detected";
        statusLbl.setText(status + " (" + Math.round(conf * 100) + "%)");

        String url = cat.getLastImageUrl(); // musst du in CatSensor speichern
        if (url == null || url.isBlank()) return;

        // Bild nur updaten, wenn URL sich geändert hat (=> alle 10s)
        String lastUrl = catLastShownUrl.get(deviceId);
        if (url.equals(lastUrl)) return;

        catLastShownUrl.put(deviceId, url);

        // backgroundLoading=true, damit es nicht ruckelt
        javafx.scene.image.Image img = new javafx.scene.image.Image(url, 220, 0, true, true, true);
        imgView.setImage(img);
    }

    private void handleConfigureActuator(Device actuatorDevice) {
        String type = actuatorDevice.getTypeLabel();

        //TODO: weitere Aktoren hinzufügen
        if ("Ventilation".equals(type)) {
            showVentilationConfig(actuatorDevice);
        } else if ("AlarmSystem".equals(type)) {
            showAlarmConfig(actuatorDevice);
        } else {
            UIUtils.styledAlert(Alert.AlertType.INFORMATION, "No configuration available for: " + type, ButtonType.OK).showAndWait();
        }
    }

    private String formatSensorValue(String typeLabel, double value) {
        if (typeLabel == null) return String.format("%.2f", value);

        if (typeLabel.equalsIgnoreCase("CO2Sensor")) {
            return String.format("%.0f", value);
        }
        // dB 1 Nachkommastelle
        if (typeLabel.equalsIgnoreCase("NoiseSensor")) {
            return String.format("%.1f", value);
        }
        return String.format("%.2f", value);
    }

    private void handleDeleteDevice(Device d) {
        Alert confirm = UIUtils.styledConfirm("Delete \"" + d.getLabel() + "\"?");
        confirm.setTitle("Delete Device");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                deviceRepository.deleteDevice(d.getId());
                loadDevicesAndRegisterSensors();
                renderDevices();
            }
        });
    }

    private void stopLiveRefresh() {
        if (liveRefresh != null) liveRefresh.stop();
    }

    public void handleDashboard() {
        stopLiveRefresh();
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    public void handleAddDevice() {
        Room room = Session.getSelectedRoom();

        var categories = List.of(Device.DeviceCategory.SENSOR, Device.DeviceCategory.ACTUATOR);
        ChoiceDialog<Device.DeviceCategory> catDialog =
                UIUtils.styledChoiceDialog(Device.DeviceCategory.SENSOR, categories, "Category:");

        catDialog.setTitle("Add Device");

        catDialog.showAndWait().ifPresent(category -> {

            List<DeviceType> types = deviceRepository.getDeviceTypesByCategory(category);
            if (types.isEmpty()) {
                UIUtils.styledAlert(Alert.AlertType.INFORMATION,
                        "No device types found for " + category, ButtonType.OK).showAndWait();
                return;
            }

            ChoiceDialog<DeviceType> typeDialog =
                    UIUtils.styledChoiceDialog(types.get(0), types, "Type:");
            typeDialog.setTitle("Add Device");

            typeDialog.showAndWait().ifPresent(chosenType -> {

                TextInputDialog nameDialog = UIUtils.styledTextInputDialog("Device name:");
                nameDialog.setTitle("Device Name");

                nameDialog.showAndWait().ifPresent(devLabel -> {
                    if (devLabel.isBlank()) return;

                    Device newDev = new Device() {
                    };
                    newDev.setLabel(devLabel);

                    int deviceId = deviceRepository.createDevice(newDev, room);
                    if (deviceId == 0) {
                        UIUtils.styledAlert(Alert.AlertType.ERROR,
                                "Could not create device.", ButtonType.OK).showAndWait();
                        return;
                    }

                    if (category == Device.DeviceCategory.SENSOR) {
                        deviceRepository.attachSensor(deviceId, chosenType.getId());
                    } else {
                        deviceRepository.attachActuator(deviceId, chosenType.getId());
                    }

                    loadDevicesAndRegisterSensors();
                    renderDevices();
                });
            });
        });
    }

    public void handleUserProfile() {
        Session.setPreviousPage(Page.DASHBOARD.fxml());
        navigate.goTo(Page.PROFILE.fxml());
    }

    public void handleLogout() {
        dialog.info("Logout", "You have been logged out.");
        navigate.goTo(Page.LOGIN.fxml());
    }

    private void showVentilationConfig(Device actuatorDevice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configure Ventilation");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
        );

        VentilationConfig cfg = actuatorCfg.getOrCreateVentilationConfig(actuatorDevice.getId());

        CheckBox autoMode = new CheckBox("Auto mode (based on CO₂)");
        autoMode.setSelected(cfg.isAutoMode());

        Spinner<Integer> onTh = new Spinner<>(400, 3000, cfg.getOnThresholdPpm());
        onTh.setEditable(true);

        Spinner<Integer> offTh = new Spinner<>(400, 3000, cfg.getOffThresholdPpm());
        offTh.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Auto mode:"), 0, 0);
        grid.add(autoMode, 1, 0);

        grid.add(new Label("Switch ON at (ppm):"), 0, 1);
        grid.add(onTh, 1, 1);

        grid.add(new Label("Switch OFF at (ppm):"), 0, 2);
        grid.add(offTh, 1, 2);

        onTh.setEditable(false);
        offTh.setEditable(false);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            int onVal = onTh.getValue();
            int offVal = offTh.getValue();

            //off muss < on sein
            if (offVal >= onVal) {
                UIUtils.styledAlert(Alert.AlertType.ERROR,
                        "OFF threshold must be smaller than ON threshold.",
                        ButtonType.OK).showAndWait();
                return;
            }

            cfg.setAutoMode(autoMode.isSelected());
            cfg.setOnThresholdPpm(onVal);
            cfg.setOffThresholdPpm(offVal);

            actuatorCfg.saveVentilationConfig(actuatorDevice.getId(), cfg);

            UIUtils.styledAlert(Alert.AlertType.INFORMATION,
                    "Saved ventilation config.", ButtonType.OK).showAndWait();
        });
    }

    private void showAlarmConfig(Device actuatorDevice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configure Alarm System");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
        );

        var cfg = actuatorCfg.getOrCreateAlarmConfig(actuatorDevice.getId());

        CheckBox autoMode = new CheckBox("Auto mode (based on Noise)");
        autoMode.setSelected(cfg.isAutoMode());

        Spinner<Integer> thresholdDb = new Spinner<>(0, 120, cfg.getNoiseThresholdDb());
        thresholdDb.setEditable(true);

        Spinner<Integer> ticks = new Spinner<>(1, 10, cfg.getRequiredConsecutiveTicks());
        ticks.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Auto mode:"), 0, 0);
        grid.add(autoMode, 1, 0);

        grid.add(new Label("Noise threshold (dB):"), 0, 1);
        grid.add(thresholdDb, 1, 1);

        grid.add(new Label("Ticks required:"), 0, 2);
        grid.add(ticks, 1, 2);

        thresholdDb.setEditable(false);
        ticks.setEditable(false);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            cfg.setAutoMode(autoMode.isSelected());
            cfg.setNoiseThresholdDb(thresholdDb.getValue());
            cfg.setRequiredConsecutiveTicks(ticks.getValue());

            actuatorCfg.saveAlarmConfig(actuatorDevice.getId(), cfg);

            UIUtils.styledAlert(Alert.AlertType.INFORMATION,
                    "Saved alarm config.", ButtonType.OK).showAndWait();
        });
    }

    private void evaluateAutomation() {

        // Ventilation Automation (CO2)

        Sensor co2 = null;
        for (Device d : devices) {
            if (d instanceof Sensor s && "CO2Sensor".equalsIgnoreCase(d.getTypeLabel())) {
                co2 = s;
                break;
            }
        }

        Device ventilation = null;
        for (Device d : devices) {
            if (d.getCategory() == Device.DeviceCategory.ACTUATOR
                    && "Ventilation".equalsIgnoreCase(d.getTypeLabel())) {
                ventilation = d;
                break;
            }
        }

        if (co2 != null && ventilation != null) {
            VentilationConfig vCfg = actuatorCfg.getOrCreateVentilationConfig(ventilation.getId());
            if (vCfg.isAutoMode()) {

                double co2Value = co2.getValue();
                String currentState = actuatorService.getStateOrDefault(ventilation.getId(), "OFF");
                boolean isOn = "ON".equalsIgnoreCase(currentState);

                if (!isOn && co2Value >= vCfg.getOnThresholdPpm()) {
                    actuatorService.setState(ventilation.getId(), "ON");
                } else if (isOn && co2Value <= vCfg.getOffThresholdPpm()) {
                    actuatorService.setState(ventilation.getId(), "OFF");
                }
            }
        }
        // Alarm Automation (Noise -> TRIGGERED)
        Sensor noise = null;
        for (Device d : devices) {
            if (d instanceof Sensor s && "NoiseSensor".equalsIgnoreCase(d.getTypeLabel())) {
                noise = s;
                break;
            }
        }

        Device alarm = null;
        for (Device d : devices) {
            if (d.getCategory() == Device.DeviceCategory.ACTUATOR
                    && "AlarmSystem".equalsIgnoreCase(d.getTypeLabel())) {
                alarm = d;
                break;
            }
        }

        if (noise == null || alarm == null) return;

        double noiseValue = noise.getValue();

        AlarmConfig alarmCfg = actuatorCfg.getOrCreateAlarmConfig(alarm.getId());
        if (!alarmCfg.isAutoMode()) return;

        String alarmState = actuatorService.getStateOrDefault(alarm.getId(), "DISARMED");
        boolean armed = "ARMED".equalsIgnoreCase(alarmState);
        boolean triggered = "TRIGGERED".equalsIgnoreCase(alarmState);

        if (!armed || triggered) {
            actuatorCfg.resetAlarmNoiseCounter(alarm.getId());
            lastAlarmState = alarmState;
            return;
        }

        int threshold = alarmCfg.getNoiseThresholdDb();
        int requiredTicks = alarmCfg.getRequiredConsecutiveTicks();

        int counter = actuatorCfg.getAlarmNoiseCounter(alarm.getId());
        counter = (noiseValue >= threshold) ? (counter + 1) : 0;
        actuatorCfg.setAlarmNoiseCounter(alarm.getId(), counter);

        if (counter >= requiredTicks) {
            actuatorService.setState(alarm.getId(), "TRIGGERED");
            actuatorCfg.resetAlarmNoiseCounter(alarm.getId());

            if (!"TRIGGERED".equalsIgnoreCase(lastAlarmState)) {
                lastAlarmState = "TRIGGERED";
                onAlarmTriggered(noiseValue);
            }
        }
    }

    private void onAlarmTriggered(double noiseValue) {
        UIUtils.showAlarmPopup(
                "ALARM!",
                "Alarmanlage ausgelöst!",
                noiseValue
        );
    }

    private String resolveDisplayUnit(Device device) {
        if (device instanceof Thermometer t) {
            return t.getDisplayUnit(); // °C or °F (instance-specific)
        }
        return device.getUnit(); // default from DeviceType
    }

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
            renderDevices(); // refreshes value display
        });

        HBox box = new HBox(8, new Label("Unit: "), celsius, fahrenheit);
        box.getStyleClass().add("muted");

        return box;
    }
}
