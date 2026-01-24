package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.app.MainApp;
import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.model.sensor.*;
import at.jku.se.gruppe2.persistence.DeviceRepository;
import at.jku.se.gruppe2.service.*;
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

import java.util.ArrayList;
import java.util.List;

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
    private final ActuatorConfigDialog actuatorConfigDialog = new ActuatorConfigDialog(actuatorCfg);
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
        setLabel();
        Room room = Session.getSelectedRoom();
        if (room == null) throw new IllegalStateException("Room selected is null");
        devices = roomDevicesService.loadDevicesAndRegisterSensors(room);
        renderDevices();

        //Alle 2 Sekunden neu rendern (gleich wie Simulation, alle 2 Sekunden neue Werte erzeugen)
        liveRefresh = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            roomAutomationService.evaluateAutomation(devices);
            renderDevices();
        }));
        liveRefresh.setCycleCount(Timeline.INDEFINITE);
        liveRefresh.play();
    }

    private void setLabel() {
        String room = Session.getSelectedRoom().getRoomLabel();

        roomLabel.setText(room);
    }

    private void renderDevices() {
        cardsFlow.getChildren().clear();
        for (Device device : devices) {
            cardsFlow.getChildren().add(deviceCardFactory.createDeviceCard(device));
        }
    }

    private void handleDeleteDevice(Device d) {
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
}
