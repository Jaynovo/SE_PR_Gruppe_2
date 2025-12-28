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
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class RoomDashboardController {

    @FXML public Label roomLabel;
    @FXML private FlowPane cardsFlow;

    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();
    private final DeviceRepository deviceRepository = new DeviceRepository();
    private final ActuatorService actuatorService = new ActuatorService();
    private final ActuatorConfigService actuatorCfg = new ActuatorConfigService();

    private final SensorSimulationService sensorSim = MainApp.getSensorSim();
    private Timeline liveRefresh;

    private List<Device> devices = new ArrayList<>();

    public void initialize () {
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

        HBox actions = new HBox(8);

        // DELETE (immer)
        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> handleDeleteDevice(device));
        actions.getChildren().add(deleteBtn);

        // ACTUATOR Controls (nur wenn Aktor)
        if (device.getCategory() == Device.DeviceCategory.ACTUATOR) {

            // ON/OFF Toggle
            ToggleButton toggle = new ToggleButton();
            toggle.getStyleClass().add("actuator-toggle");

            String currentState = actuatorService.getStateOrDefault(device.getId(),"OFF");

            boolean isOn = "ON".equalsIgnoreCase(currentState);
            toggle.setSelected(isOn);
            toggle.setText(isOn ? "ON" : "OFF");

            toggle.selectedProperty().addListener((obs, oldVal, on) -> {
                String newState = on ? "ON" : "OFF";
                toggle.setText(newState);
                actuatorService.setState(device.getId(), newState);
            });

            // Configure
            Button configBtn = new Button("Configure");
            configBtn.setOnAction(e -> handleConfigureActuator(device));

            actions.getChildren().addAll(toggle, configBtn);
        }

        deviceCard.getChildren().add(actions);
        return deviceCard;
    }

    private void handleConfigureActuator(Device actuatorDevice) {
        String type = actuatorDevice.getTypeLabel();

        //TODO: weitere Aktoren hinzufügen
        if ("Ventilation".equals(type)) {
            showVentilationConfig(actuatorDevice);
        } else if ("AlarmSystem".equals(type)) {
            //showAlarmConfig(actuatorDevice);
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

                    Device newDev = new Device() {};
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
        Session.setPreviousPage(Page.HOME_DASHBOARD.fxml());
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

    private void evaluateAutomation() {
        Room room = Session.getSelectedRoom();

        // 1) CO2 Sensor im Raum finden
        Sensor co2 = null;
        for (Device d : devices) {
            if (d instanceof Sensor s && "CO2Sensor".equalsIgnoreCase(d.getTypeLabel())) {
                co2 = s;
                break;
            }
        }
        if (co2 == null) return; // kein CO2 Sensor -> keine Lüftungsautomatik

        double co2Value = co2.getValue();

        // 2) Ventilation Actuator im Raum finden
        Device ventilation = null;
        for (Device d : devices) {
            if (d.getCategory() == Device.DeviceCategory.ACTUATOR
                    && "Ventilation".equalsIgnoreCase(d.getTypeLabel())) {
                ventilation = d;
                break;
            }
        }
        if (ventilation == null) return; // kein Ventilator im Raum

        // 3) Config holen
        VentilationConfig cfg = actuatorCfg.getOrCreateVentilationConfig(ventilation.getId());
        if (!cfg.isAutoMode()) return; // User will manuell steuern

        // 4) aktuellen State holen
        String currentState = actuatorService.getStateOrDefault(ventilation.getId(), "OFF");
        boolean isOn = "ON".equalsIgnoreCase(currentState);

        // 5) Hysterese-Regeln anwenden
        if (!isOn && co2Value >= cfg.getOnThresholdPpm()) {
            actuatorService.setState(ventilation.getId(), "ON");
        } else if (isOn && co2Value <= cfg.getOffThresholdPpm()) {
            actuatorService.setState(ventilation.getId(), "OFF");
        }
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
