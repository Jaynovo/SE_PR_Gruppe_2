package at.jku.se.gruppe2.presentation.component.custom;

import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.config.VentilationConfig;
import at.jku.se.gruppe2.domain.service.device.ActuatorConfigService;
import at.jku.se.gruppe2.presentation.util.UIUtils;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class ActuatorConfigDialog {
    private final ActuatorConfigService actuatorCfg;

    public ActuatorConfigDialog(ActuatorConfigService actuatorCfg) {
        this.actuatorCfg = actuatorCfg;
    }

    public void show(Device actuatorDevice) {
        String type = actuatorDevice.getTypeLabel();

        //TODO: weitere Aktoren hinzufügen and change logic to switch/case
        switch (type) {
            case "Ventilation":
                showVentilationConfig(actuatorDevice);
                break;
            case "AlarmSystem":
                showAlarmConfig(actuatorDevice);
                break;
            default:
                UIUtils.styledAlert(Alert.AlertType.INFORMATION, "No configuration available for: " + type, ButtonType.OK).showAndWait();
        }
    }

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

            // OFF muss < ON sein (CO2)
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