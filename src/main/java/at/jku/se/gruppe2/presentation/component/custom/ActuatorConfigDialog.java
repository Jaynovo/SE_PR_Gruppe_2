package at.jku.se.gruppe2.presentation.component.custom;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.config.BlindsConfig;
import at.jku.se.gruppe2.domain.model.device.config.VentilationConfig;
import at.jku.se.gruppe2.domain.service.device.ActuatorConfigService;
import at.jku.se.gruppe2.presentation.util.UIUtils;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * Dialog component for configuring actuator devices in the smart home system.
 *
 * <p>This dialog presents device-specific configuration interfaces based on the
 * actuator type.
 *
 * <p>Each configuration type provides validation logic to ensure sensible threshold
 * values and displays appropriate error messages for invalid input.</p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * ActuatorConfigDialog dialog = new ActuatorConfigDialog(actuatorConfigService);
 * dialog.show(ventilationDevice);
 * }</pre>
 *
 * @see ActuatorConfigService
 * @see Device
 */
public class ActuatorConfigDialog {
    private final ActuatorConfigService actuatorCfg;

    /**
     * Constructs a new actuator configuration dialog.
     *
     * @param actuatorCfg service for managing actuator configurations (must not be {@code null})
     * @throws NullPointerException if {@code actuatorCfg} is {@code null}
     */
    public ActuatorConfigDialog(ActuatorConfigService actuatorCfg) {
        this.actuatorCfg = actuatorCfg;
    }

    /**
     * Displays the appropriate configuration dialog based on the actuator device type.
     *
     * <p>The dialog type is determined by the device's type label:</p>
     * <ul>
     *   <li>"Ventilation" → {@link #showVentilationConfig(Device)}</li>
     *   <li>"AlarmSystem" → {@link #showAlarmConfig(Device)}</li>
     *   <li>"Blinds" → {@link #showBlindsConfig(Device)}</li>
     *   <li>Other types → Information alert indicating no configuration available</li>
     * </ul>
     *
     * @param actuatorDevice the actuator device to configure (must not be {@code null})
     * @throws NullPointerException if {@code actuatorDevice} is {@code null}
     */
    public void show(Device actuatorDevice) {
        String type = actuatorDevice.getTypeLabel();

        switch (type) {
            case "Ventilation":
                showVentilationConfig(actuatorDevice);
                break;
            case "AlarmSystem":
                showAlarmConfig(actuatorDevice);
                break;
            case "BlindsActuator":
                showBlindsConfig(actuatorDevice);
                break;
            default:
                UIUtils.styledAlert(Alert.AlertType.INFORMATION, "No configuration available for: " + type, ButtonType.OK).showAndWait();
        }
    }

    /**
     * Displays the configuration dialog for blinds actuators.
     *
     * <p>Allows configuration of:</p>
     * <ul>
     *   <li>Auto mode (enabled/disabled)</li>
     *   <li>Light threshold for closing blinds (in Lux)</li>
     *   <li>Light threshold for opening blinds (in Lux)</li>
     * </ul>
     *
     * <p><b>Validation:</b> The open threshold must be strictly less than the close
     * threshold to prevent ambiguous behavior. An error alert is shown if this
     * constraint is violated.</p>
     *
     * <p>Configuration changes are persisted only if the user confirms with OK.</p>
     *
     * @param actuatorDevice the blinds actuator device to configure (must not be {@code null})
     * @throws NullPointerException if {@code actuatorDevice} is {@code null}
     */
    private void showBlindsConfig(Device actuatorDevice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configure Blinds");
        dialog.getDialogPane().getStylesheets().add(
                actuatorCfg.getClass().getResource("/css/app.css").toExternalForm()
        );

        BlindsConfig cfg = actuatorCfg.getOrCreateBlindsConfig(actuatorDevice.getId());

        CheckBox autoMode = new CheckBox("Auto mode (based on Light)");
        autoMode.setSelected(cfg.isAutoMode());

        Spinner<Double> closeLux = new Spinner<>(0.0, 100_000.0, cfg.getCloseAtLux(), 50);
        Spinner<Double> openLux  = new Spinner<>(0.0, 100_000.0, cfg.getOpenAtLux(), 50);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(autoMode, 0, 0, 2, 1);
        grid.add(new Label("Close blinds above (Lux):"), 0, 1);
        grid.add(closeLux, 1, 1);
        grid.add(new Label("Open blinds below (Lux):"), 0, 2);
        grid.add(openLux, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            if (openLux.getValue() >= closeLux.getValue()) {
                UIUtils.styledAlert(
                        Alert.AlertType.ERROR,
                        "Open-Lux must be smaller than Close-Lux",
                        ButtonType.OK
                ).showAndWait();
                return;
            }

            cfg.setAutoMode(autoMode.isSelected());
            cfg.setCloseAtLux(closeLux.getValue());
            cfg.setOpenAtLux(openLux.getValue());

            actuatorCfg.saveBlindsConfig(actuatorDevice.getId(), cfg);
        });
    }

    /**
     * Displays the configuration dialog for ventilation actuators.
     *
     * <p>Allows configuration of:</p>
     * <ul>
     *   <li>Auto mode (enabled/disabled)</li>
     *   <li>CO₂ thresholds: ON threshold (400-3000 ppm) and OFF threshold (400-3000 ppm)</li>
     *   <li>Humidity thresholds: ON threshold (20-90%) and OFF threshold (20-90%)</li>
     * </ul>
     *
     * <p><b>Validation:</b></p>
     * <ul>
     *   <li>OFF threshold must be strictly less than ON threshold for CO₂</li>
     *   <li>OFF threshold must be strictly less than ON threshold for humidity</li>
     * </ul>
     *
     * <p>Configuration changes are persisted only if the user confirms with OK and
     * all validation passes. A success confirmation is shown after saving.</p>
     *
     * @param actuatorDevice the ventilation actuator device to configure (must not be {@code null})
     * @throws NullPointerException if {@code actuatorDevice} is {@code null}
     */
    public void showVentilationConfig(Device actuatorDevice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configure Ventilation");
        dialog.getDialogPane().getStylesheets().add(
                actuatorCfg.getClass().getResource("/css/app.css").toExternalForm()
        );

        VentilationConfig cfg = actuatorCfg.getOrCreateVentilationConfig(actuatorDevice.getId());

        CheckBox autoMode = new CheckBox("Auto mode (CO₂ / Humidity)");
        autoMode.setSelected(cfg.isAutoMode());

        // CO2 thresholds (ppm)
        Spinner<Integer> onTh = new Spinner<>(400, 3000, cfg.getOnThresholdPpm(), 50);
        Spinner<Integer> offTh = new Spinner<>(400, 3000, cfg.getOffThresholdPpm(), 50);

        // Humidity thresholds (%)
        Spinner<Double> humOnTh = new Spinner<>(20.0, 90.0, cfg.getOnThresholdHumidity(), 0.5);
        Spinner<Double> humOffTh = new Spinner<>(20.0, 90.0, cfg.getOffThresholdHumidity(), 0.5);

        onTh.setEditable(false);
        offTh.setEditable(false);
        humOnTh.setEditable(false);
        humOffTh.setEditable(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Auto mode:"), 0, 0);
        grid.add(autoMode, 1, 0);

        grid.add(new Label("Switch ON at (ppm):"), 0, 1);
        grid.add(onTh, 1, 1);

        grid.add(new Label("Switch OFF at (ppm):"), 0, 2);
        grid.add(offTh, 1, 2);

        grid.add(new Label("Switch ON at (%):"), 0, 3);
        grid.add(humOnTh, 1, 3);

        grid.add(new Label("Switch OFF at (%):"), 0, 4);
        grid.add(humOffTh, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            int onVal = onTh.getValue();
            int offVal = offTh.getValue();

            double humOnVal = humOnTh.getValue();
            double humOffVal = humOffTh.getValue();

            // OFF muss < ON sein (CO₂)
            if (offVal >= onVal) {
                UIUtils.styledAlert(Alert.AlertType.ERROR,
                        "OFF threshold (CO₂) must be smaller than ON threshold.",
                        ButtonType.OK).showAndWait();
                return;
            }

            // OFF muss < ON sein (Humidity)
            if (humOffVal >= humOnVal) {
                UIUtils.styledAlert(Alert.AlertType.ERROR,
                        "OFF threshold (Humidity) must be smaller than ON threshold.",
                        ButtonType.OK).showAndWait();
                return;
            }

            cfg.setAutoMode(autoMode.isSelected());
            cfg.setOnThresholdPpm(onVal);
            cfg.setOffThresholdPpm(offVal);
            cfg.setOnThresholdHumidity(humOnVal);
            cfg.setOffThresholdHumidity(humOffVal);

            actuatorCfg.saveVentilationConfig(actuatorDevice.getId(), cfg);


            UIUtils.styledAlert(Alert.AlertType.INFORMATION,
                    "Saved ventilation config.", ButtonType.OK).showAndWait();
        });
    }

    /**
     * Displays the configuration dialog for alarm system actuators.
     *
     * <p>Allows configuration of:</p>
     * <ul>
     *   <li>Auto mode (enabled/disabled)</li>
     *   <li>Noise threshold in decibels (0-120 dB)</li>
     *   <li>Required consecutive ticks before triggering alarm (1-10)</li>
     * </ul>
     *
     * <p>The consecutive ticks requirement helps prevent false alarms from brief
     * noise spikes by requiring sustained high noise levels.</p>
     *
     * <p>Configuration changes are persisted only if the user confirms with OK.
     * A success confirmation is shown after saving.</p>
     *
     * @param actuatorDevice the alarm system actuator device to configure (must not be {@code null})
     * @throws NullPointerException if {@code actuatorDevice} is {@code null}
     */
    public void showAlarmConfig(Device actuatorDevice) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle("Configure Alarm System");
        dialog.getDialogPane().getStylesheets().add(
                actuatorCfg.getClass().getResource("/css/app.css").toExternalForm()
        );

        var cfg = actuatorCfg.getOrCreateAlarmConfig(actuatorDevice.getId());

        CheckBox autoMode = new CheckBox("Auto mode (based on Noise)");
        autoMode.setSelected(cfg.isAutoMode());

        Spinner<Integer> thresholdDb = new Spinner<Integer>(0, 120, cfg.getNoiseThresholdDb());
        thresholdDb.setEditable(true);

        Spinner<Integer> ticks = new Spinner<Integer>(1, 10, cfg.getRequiredConsecutiveTicks());
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
}