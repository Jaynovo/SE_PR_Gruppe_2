package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.sensor.*;
import at.jku.se.gruppe2.model.user.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.SensorSimulationService;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.service.actuator.*;
import at.jku.se.gruppe2.service.actuator.ActuatorService;
import at.jku.se.gruppe2.service.actuator.ActuatorConfigService;
import at.jku.se.gruppe2.service.user.*;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.component.DeviceCardFactory;
import at.jku.se.gruppe2.ui.custom.ActuatorConfigDialog;
import at.jku.se.gruppe2.ui.navigation.Page;
import at.jku.se.gruppe2.utils.Session;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.scene.image.*;

import java.util.*;

public class RoomDashboardController {

    @FXML    public Label roomLabel;
    @FXML    private FlowPane cardsFlow;
    @FXML    private Button addDeviceButton;

    //private static final Duration REFRESH_INTERVAL = Duration.millis(500);

    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();
    private final DeviceRepository deviceRepository = new DeviceRepository();
    private final ActuatorService actuatorService = new ActuatorService();
    private final ActuatorConfigService actuatorCfg = new ActuatorConfigService();
    private final ActuatorConfigDialog actuatorConfigDialog = new ActuatorConfigDialog(actuatorCfg);
    private final ActuatorPermissionService actuatorPermService = new ActuatorPermissionService();
    private final AuthorizationService authService = new AuthorizationService();
    private final java.util.Map<Integer, javafx.scene.image.ImageView> catImageViews = new java.util.HashMap<>();
    private final java.util.Map<Integer, javafx.scene.control.Label> catStatusLabels = new java.util.HashMap<>();
    private final java.util.Map<Integer, String> catLastShownUrl = new java.util.HashMap<>();


    private final SensorSimulationService sensorSim = MainApp.getSensorSim();
    private final RoomDevicesService roomDevicesService = new RoomDevicesService(sensorSim);
    private final RoomAutomationService roomAutomationService = new RoomAutomationService(actuatorService, actuatorCfg);
    private final DeviceCardFactory deviceCardFactory = new DeviceCardFactory(
            actuatorCfg, actuatorService,
            this::handleDeleteDevice, actuatorConfigDialog::show,
            this::renderDevices);
    private Timeline liveRefresh;

    private List<Device> devices = new ArrayList<>();

    public void initialize() {
        Room room = Session.getSelectedRoom();

        if (room != null && room.getHome() == null) {
            // Load the home for this room
            HomeRepository homeRepo = new HomeRepository();
            User currentUser = Session.getCurrentUser();
            if (currentUser != null) {
                Home home = homeRepo.getHomeByUser(currentUser).orElse(null);
                room.setHome(home);
                Session.setSelectedRoom(room);  // Update session with complete room
            }
        }

        setLabel();
        loadDevicesAndRegisterSensors();
        updateUIBasedOnPermissions();
        renderDevices();

        //Alle 2 Sekunden neu rendern (gleich wie Simulation, alle 2 Sekunden neue Werte erzeugen)
        liveRefresh = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            roomAutomationService.evaluateAutomation(devices);
            renderDevices();
        }));
        liveRefresh.setCycleCount(Timeline.INDEFINITE);
        liveRefresh.play();
    }

    private void updateUIBasedOnPermissions() {
        Room room = Session.getSelectedRoom();
        if (room == null || room.getHome() == null) {
            return;
        }

        int homeId = room.getHome().getId();

        // Only residents and owners can add devices
        if (addDeviceButton != null) {
            boolean canAddDevices = authService.canAddDevices(homeId);
            addDeviceButton.setVisible(canAddDevices);
            addDeviceButton.setManaged(canAddDevices);

            if (!canAddDevices) {
                addDeviceButton.setTooltip(new Tooltip("Only residents and owners can add devices"));
            }
        }
    }

    private void setLabel() {
        String room = Session.getSelectedRoom().getRoomLabel();
        roomLabel.setText(room);
    }

    private void renderDevices() {
        cardsFlow.getChildren().clear();

        Room room = Session.getSelectedRoom();
        if (room == null || room.getHome() == null) {
            return;
        }

        int homeId = room.getHome().getId();
        boolean canDelete = authService.canRemoveDevices(homeId);
        boolean canConfigure = authService.canConfigureActuators(homeId);

        for (Device device : devices) {
            cardsFlow.getChildren().add(createDeviceCard(device, canDelete, canConfigure));
        }
    }

    private Pane createDeviceCard(Device device, boolean canDelete, boolean canConfigure) {
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
                    l.getStyleClass().add("badge");
                    return l;
                });

                // ImageView (einmalig)
                javafx.scene.image.ImageView imgView = catImageViews.computeIfAbsent(device.getId(), id -> {
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
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
            } else {
                //CO2/Noise/...
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

        // DELETE button only shows if user has permission
        if (canDelete) {
            Button deleteBtn = new Button("Delete");
            deleteBtn.setOnAction(e -> handleDeleteDevice(device));
            actions.getChildren().add(deleteBtn);
        }

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

                // CONFIGURE button only shows if user has permission
                Button configBtn = new Button("Configure");
                configBtn.setVisible(canConfigure);
                configBtn.setManaged(canConfigure);
                configBtn.setOnAction(e -> handleConfigureActuator(device));

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
                        deleteBtn.setOnAction(e -> handleDeleteDevice(device));
                        row2.getChildren().add(deleteBtn);
                    }
                    actionBox.getChildren().addAll(row1, row2);
                } else {
                    actionBox.getChildren().add(row1);
                }

                deviceCard.getChildren().add(actionBox);
                return deviceCard;

            }
            if ("Cat Feeder".equalsIgnoreCase(typeLabel)) {

                // Status aus Service holen
                String feederState = actuatorService.getStateOrDefault(device.getId(), "READY");
                int cd = actuatorCfg.getCatFeederCooldown(device.getId()); // ticks
                String stateText = feederState;

                // Cooldown anzeigen
                if ("COOLDOWN".equalsIgnoreCase(feederState) && cd > 0) {
                    stateText = "COOLDOWN (" + (cd * 2) + "s)";
                }

                // Status-Button (nur Anzeige)
                Button statusBtn = new Button(stateText);
                statusBtn.setDisable(true);

                // Basis-Klasse
                statusBtn.getStyleClass().add("feeder-status");

                // alte State-Klassen entfernen (wichtig wegen Re-Render!)
                statusBtn.getStyleClass().removeAll(
                        "feeder-ready",
                        "feeder-feeding",
                        "feeder-cooldown"
                );
                // State-spezifische Klasse setzen
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

                // Farbe je nach State
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
                configBtn.setOnAction(e -> handleConfigureActuator(device));

                // Layout: Status oben, darunter Configure/Delete
                VBox actionBox = new VBox(8);
                HBox row = new HBox(8);

                if (canDelete) {
                    Button deleteBtn = new Button("Delete");
                    deleteBtn.setOnAction(e -> handleDeleteDevice(device));
                    row.getChildren().addAll(configBtn, deleteBtn);
                    actionBox.getChildren().addAll(statusBtn, row);
                }

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

                actions.getChildren().add(toggle);

                // CONFIGURE button only shows if user has permission
                if (canConfigure) {
                    Button configBtn = new Button("Configure");
                    configBtn.setOnAction(e -> handleConfigureActuator(device));
                    actions.getChildren().add(configBtn);
                }
            }
        }

        deviceCard.getChildren().add(actions);
        return deviceCard;
    }
    private void setImageCover(ImageView iv, Image img, double w, double h) {
        iv.setImage(img);
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(true);

        // Warten bis Bild geladen ist, dann viewport crop setzen
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
            // zu breit -> links/rechts weg
            cropH = imgH;
            cropW = imgH * targetRatio;
        } else {
            // zu hoch -> oben/unten weg
            cropW = imgW;
            cropH = imgW / targetRatio;
        }

        double x = (imgW - cropW) / 2.0;
        double y = (imgH - cropH) / 2.0;

        iv.setViewport(new javafx.geometry.Rectangle2D(x, y, cropW, cropH));
    }

    private void updateCatCardUI(int deviceId, CatSensor cat, Label statusLbl, javafx.scene.image.ImageView imgView) {

        double conf = cat.getConfidence(); // 0..1
        String status = cat.isCatDetected() ? "CAT DETECTED" : "No cat detected";
        statusLbl.setText(status + " (" + Math.round(conf * 100) + "%)");

        String url = cat.getLastImageUrl();
        if (url == null || url.isBlank()) return;

        // Bild nur updaten, wenn URL sich geändert hat (=> alle 10s)
        String lastUrl = catLastShownUrl.get(deviceId);
        if (url.equals(lastUrl)) return;

        catLastShownUrl.put(deviceId, url);

        // backgroundLoading=true, damit es nicht ruckelt
        Image img = new Image(url, 0, 0, true, true, true);
        setImageCover(imgView,img,180,120);

        statusLbl.getStyleClass().removeAll("badge-ok", "badge-warn", "badge-off");

        boolean detected = cat.isCatDetected();
        if (detected) statusLbl.getStyleClass().add("badge-ok");
        else statusLbl.getStyleClass().add("badge-off");
    }

    private void handleConfigureActuator(Device actuatorDevice) {
        // CHECK permission
        if (!actuatorPermService.canConfigureActuator(actuatorDevice.getId())) {
            dialog.error("Permission Denied",
                    "Only residents and owners can configure actuators.");
            return;
        }

        String type = actuatorDevice.getTypeLabel();

        //TODO: weitere Aktoren hinzufügen
        if ("Ventilation".equals(type)) {
            showVentilationConfig(actuatorDevice);
        } else if ("AlarmSystem".equals(type)) {
            showAlarmConfig(actuatorDevice);
        } else if ("Cat Feeder".equals(type)) {
            showCatFeederConfig(actuatorDevice);
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
        // CHECK permission
        Room room = Session.getSelectedRoom();
        if (room != null && room.getHome() != null) {
            if (!authService.canRemoveDevices(room.getHome().getId())) {
                dialog.error("Permission Denied",
                        "Only residents and owners can delete devices.");
                return;
            }
        }

        Alert confirm = UIUtils.styledConfirm("Delete \"" + d.getLabel() + "\"?");
        confirm.setTitle("Delete Device");
        Room room = Session.getSelectedRoom();

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                deviceRepository.deleteDevice(d.getId());
                devices = roomDevicesService.loadDevicesAndRegisterSensors(room);
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

        // CHECK permission
        if (room != null && room.getHome() != null) {
            if (!authService.canAddDevices(room.getHome().getId())) {
                dialog.error("Permission Denied",
                        "Only residents and owners can add devices.");
                return;
            }
        }

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

                    devices = roomDevicesService.loadDevicesAndRegisterSensors(room);
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

    public List<Device> getDevices() {
        return devices;
    }

    public DeviceRepository getDeviceRepository() {
        return deviceRepository;
    }

    public SensorSimulationService getSensorSim() {
        return sensorSim;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public ActuatorService getActuatorService() {
        return actuatorService;
    }

    public ActuatorConfigService getActuatorCfg() {
        return actuatorCfg;
    }

    public ActuatorConfigDialog getActuatorConfigDialog() {
        return actuatorConfigDialog;
    }

    private void showCatFeederConfig(Device actuatorDevice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configure Cat Feeder");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
        );

        CatFeederConfig cfg = actuatorCfg.getOrCreateCatFeederConfig(actuatorDevice.getId());

        // Confidence als Prozent (0..100)
        int currentPercent = (int) Math.round(cfg.getMinConfidence() * 100.0);

        Spinner<Integer> minConf = new Spinner<>(0, 100, currentPercent);
        minConf.setEditable(true);

        Spinner<Integer> cooldownTicks = new Spinner<>(0, 600, cfg.getCooldownTicks()); // bis 20min
        cooldownTicks.setEditable(true);

        Label cooldownInfo = new Label();
        cooldownInfo.getStyleClass().add("muted");
        cooldownInfo.setText("= " + (cooldownTicks.getValue() * 2) + " seconds");

        cooldownTicks.valueProperty().addListener((obs, oldV, newV) -> {
            int v = (newV == null) ? 0 : newV;
            cooldownInfo.setText("= " + (v * 2) + " seconds");
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Min confidence (%):"), 0, 0);
        grid.add(minConf, 1, 0);

        grid.add(new Label("Cooldown (ticks):"), 0, 1);
        grid.add(cooldownTicks, 1, 1);
        grid.add(cooldownInfo, 1, 2);

        minConf.setEditable(false);
        cooldownTicks.setEditable(false);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            int percent = minConf.getValue();
            int ticks = cooldownTicks.getValue();

            cfg.setMinConfidence(percent / 100.0);
            cfg.setCooldownTicks(ticks);

            actuatorCfg.saveCatFeederConfig(actuatorDevice.getId(), cfg);

            UIUtils.styledAlert(Alert.AlertType.INFORMATION,
                    "Saved cat feeder config.", ButtonType.OK).showAndWait();
        });
    }
}