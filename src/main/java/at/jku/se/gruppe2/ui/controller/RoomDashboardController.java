package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.actuator.CatFeederConfig;
import at.jku.se.gruppe2.model.sensor.*;
import at.jku.se.gruppe2.model.user.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.SensorSimulationService;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.service.actuator.*;
import at.jku.se.gruppe2.service.user.*;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.component.DeviceCardFactory;
import at.jku.se.gruppe2.ui.custom.ActuatorConfigDialog;
import at.jku.se.gruppe2.ui.navigation.Page;
import at.jku.se.gruppe2.utils.Session;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.*;

public class RoomDashboardController {

    @FXML
    public Label roomLabel;
    @FXML
    private FlowPane cardsFlow;
    @FXML
    private Button addDeviceButton;

    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();
    private final DeviceRepository deviceRepository = new DeviceRepository();
    private final ActuatorService actuatorService = new ActuatorService();
    private final ActuatorConfigService actuatorCfg = new ActuatorConfigService();
    private final ActuatorConfigDialog actuatorConfigDialog = new ActuatorConfigDialog(actuatorCfg);
    private final ActuatorPermissionService actuatorPermService = new ActuatorPermissionService();
    private final AuthorizationService authService = new AuthorizationService();

    private final SensorSimulationService sensorSim = MainApp.getSensorSim();
    private final RoomDevicesService roomDevicesService = new RoomDevicesService(sensorSim);
    private final RoomAutomationService roomAutomationService = new RoomAutomationService(actuatorService, actuatorCfg);

    private final DeviceCardFactory deviceCardFactory = new DeviceCardFactory(
            actuatorCfg,
            actuatorService,
            this::handleDeleteDevice,
            this::handleConfigureActuator,
            this::renderDevices
    );

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
        devices = roomDevicesService.loadDevicesAndRegisterSensors(room);
        updateUIBasedOnPermissions();
        renderDevices();

        // Refresh every 2 seconds (same as simulation, generates new values every 2 seconds)
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
            cardsFlow.getChildren().add(deviceCardFactory.createDeviceCard(device, canDelete, canConfigure));
        }
    }

    private void handleConfigureActuator(Device actuatorDevice) {
        // CHECK permission
        Room room = Session.getSelectedRoom();
        if (room == null || room.getHome() == null) {
            dialog.error("Permission Denied", "No home context available.");
            return;
        }

        int homeId = room.getHome().getId();
        if (!authService.canConfigureActuators(homeId)) {
            dialog.error("Permission Denied", "Only residents and owners can configure actuators.");
            return;
        }

        String type = actuatorDevice.getTypeLabel();

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

    // Configuration dialogs for specific actuators

    private void showVentilationConfig(Device actuatorDevice) {
        // Delegate to ActuatorConfigDialog
        actuatorConfigDialog.show(actuatorDevice);
    }

    private void showAlarmConfig(Device actuatorDevice) {
        // Delegate to ActuatorConfigDialog
        actuatorConfigDialog.show(actuatorDevice);
    }

    private void showCatFeederConfig(Device actuatorDevice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configure Cat Feeder");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
        );

        CatFeederConfig cfg = actuatorCfg.getOrCreateCatFeederConfig(actuatorDevice.getId());

        // Confidence as percentage (0..100)
        int currentPercent = (int) Math.round(cfg.getMinConfidence() * 100.0);

        Spinner<Integer> minConf = new Spinner<>(0, 100, currentPercent);
        minConf.setEditable(false);

        Spinner<Integer> cooldownTicks = new Spinner<>(0, 600, cfg.getCooldownTicks()); // up to 20min
        cooldownTicks.setEditable(false);

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

    // Getters for testing purposes
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
}