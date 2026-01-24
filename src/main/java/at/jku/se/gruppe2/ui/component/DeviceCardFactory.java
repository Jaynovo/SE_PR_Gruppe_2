package at.jku.se.gruppe2.ui.component;

import at.jku.se.gruppe2.model.Device;
import at.jku.se.gruppe2.model.sensor.Sensor;
import at.jku.se.gruppe2.model.sensor.Thermometer;
import at.jku.se.gruppe2.service.ActuatorConfigService;
import at.jku.se.gruppe2.service.ActuatorService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class DeviceCardFactory {
    private final ActuatorService actuatorService;
    private final ActuatorConfigService actuatorCfg;
    private final java.util.function.Consumer<Device> onDelete;
    private final java.util.function.Consumer<Device> onConfigure;
    private final Runnable requestRender;

    public DeviceCardFactory(ActuatorConfigService actuatorConfigService, ActuatorService actuatorService, java.util.function.Consumer<Device> onDelete, java.util.function.Consumer<Device> onConfigure, Runnable requestRender) {
        this.actuatorService = actuatorService;
        this.actuatorCfg = actuatorConfigService;
        this.onDelete = onDelete;
        this.onConfigure = onConfigure;
        this.requestRender = requestRender;
    }

    public Pane createDeviceCard(Device device) {
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

        //DELETE (immer)
        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> onDelete.accept(device));
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
                    requestRender.run(); // UI neu zeichnen
                });

                Button configBtn = new Button("Configure");
                configBtn.setOnAction(e -> onConfigure.accept(device));

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
                configBtn.setOnAction(e -> onConfigure.accept(device));

                actions.getChildren().addAll(toggle, configBtn);
            }
        }

        deviceCard.getChildren().add(actions);
        return deviceCard;
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
            requestRender.run(); // refreshes value display
        });

        HBox box = new HBox(8, new Label("Unit: "), celsius, fahrenheit);
        box.getStyleClass().add("muted");

        return box;
    }
}