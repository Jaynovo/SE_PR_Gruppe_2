package at.jku.se.gruppe2.presentation.controller.room;

import app.MainApp;
import at.jku.se.gruppe2.domain.model.device.config.CatFeederConfig;
import at.jku.se.gruppe2.domain.model.device.config.HeatingConfig;
import at.jku.se.gruppe2.domain.model.device.Device;
import at.jku.se.gruppe2.domain.model.device.DeviceType;
import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.model.home.Room;
import at.jku.se.gruppe2.domain.service.automation.ClimateControlService;
import at.jku.se.gruppe2.presentation.service.DialogService;
import at.jku.se.gruppe2.application.navigation.NavigationService;
import at.jku.se.gruppe2.domain.service.user.AuthorizationService;
import at.jku.se.gruppe2.domain.model.user.User;
import at.jku.se.gruppe2.infrastructure.persistence.repository.DeviceRepository;
import at.jku.se.gruppe2.infrastructure.persistence.repository.HomeRepository;
import at.jku.se.gruppe2.domain.service.device.SensorSimulationService;
import at.jku.se.gruppe2.domain.service.automation.RoomAutomationService;
import at.jku.se.gruppe2.domain.service.device.ActuatorConfigService;
import at.jku.se.gruppe2.domain.service.device.ActuatorPermissionService;
import at.jku.se.gruppe2.domain.service.device.ActuatorService;
import at.jku.se.gruppe2.domain.service.room.RoomDevicesService;
import at.jku.se.gruppe2.presentation.util.UIUtils;
import at.jku.se.gruppe2.presentation.component.factory.DeviceCardFactory;
import at.jku.se.gruppe2.presentation.component.custom.ActuatorConfigDialog;
import at.jku.se.gruppe2.presentation.navigation.Page;
import at.jku.se.gruppe2.infrastructure.security.Session;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.*;

/**
 * Controller for the room dashboard view showing device cards and live sensor data.
 *
 * <p>This controller handles {@code room_dashboard_page.fxml} and is responsible for:</p>
 * <ul>
 *   <li>Loading and displaying all devices in the currently selected room</li>
 *   <li>Running a live refresh cycle (every 2 seconds) to update sensor values
 *       and evaluate automation rules</li>
 *   <li>Adding and deleting devices with permission checks</li>
 *   <li>Showing actuator configuration dialogs for specific device types</li>
 *   <li>Editing device names inline</li>
 *   <li>Permission-based UI visibility (add button, edit/delete controls)</li>
 * </ul>
 *
 * <p><b>Live refresh:</b> A {@link Timeline} fires every 2 seconds to:</p>
 * <ol>
 *   <li>Evaluate automation rules ({@link RoomAutomationService})</li>
 *   <li>Evaluate climate control ({@link ClimateControlService})</li>
 *   <li>Update device card labels/controls with latest values ({@link DeviceCardFactory#updateLiveValues})</li>
 * </ol>
 *
 * <p>The live refresh must be stopped before navigating away to avoid dangling timers.</p>
 *
 * <p><b>Permission model:</b> Add, delete, configure, and edit operations each
 * check the current user's role in the home before proceeding.</p>
 *
 * @see DeviceCardFactory
 * @see RoomAutomationService
 * @see ClimateControlService
 * @see ActuatorConfigService
 */
public class RoomDashboardController {

    @FXML    public Label roomLabel;
    @FXML    private FlowPane cardsFlow;
    @FXML    private Button addDeviceButton;

    private final NavigationService navigate = new NavigationService();
    private final DialogService dialog = new DialogService();
    private final DeviceRepository deviceRepository = new DeviceRepository();
    private final ActuatorService actuatorService = new ActuatorService();
    private final ActuatorConfigService actuatorCfg = new ActuatorConfigService();
    private final ActuatorConfigDialog actuatorConfigDialog = new ActuatorConfigDialog(actuatorCfg);
    private final AuthorizationService authService = new AuthorizationService();

    private final SensorSimulationService sensorSim = MainApp.getSensorSim();
    private final RoomDevicesService roomDevicesService = new RoomDevicesService(sensorSim);
    private final RoomAutomationService roomAutomationService = new RoomAutomationService(actuatorService, actuatorCfg);
    private final ClimateControlService climateControlService = new ClimateControlService(actuatorService, actuatorCfg);

    /**
     * Factory for creating device card UI components with bound action callbacks.
     */
    private final DeviceCardFactory deviceCardFactory = new DeviceCardFactory(
            actuatorCfg,
            actuatorService,
            this::handleDeleteDevice,
            this::handleConfigureActuator,
            this::handleEditDevice,
            this::renderDevices
    );

    /**
     * Repeating timeline that updates live sensor/actuator values every 2 seconds.
     */
    private Timeline liveRefresh;

    /**
     * The current list of devices in the selected room.
     */
    private List<Device> devices = new ArrayList<>();

    /**
     * Initializes the room dashboard for the currently selected room.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Loads the selected room from {@link Session}, ensuring home context is set</li>
     *   <li>Loads all devices for the room and registers them with the simulation service</li>
     *   <li>Applies permission-based UI visibility</li>
     *   <li>Renders all device cards</li>
     *   <li>Starts the 2-second live refresh timeline</li>
     * </ol>
     *
     * <p>Returns early without any action if no room is selected in the session.</p>
     */
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
        if (room == null) return;
        setLabel();
        devices = roomDevicesService.loadDevicesAndRegisterSensors(room);
        room.setDevices(devices);
        updateUIBasedOnPermissions();
        renderDevices();

        // Refresh every 2 seconds (same as simulation, generates new values every 2 seconds)
        liveRefresh = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            roomAutomationService.evaluateAutomation(devices);

            Room currentRoom = Session.getSelectedRoom();
            if (currentRoom != null) {
                currentRoom.setDevices(devices);
                climateControlService.evaluate(currentRoom);
            }

            deviceCardFactory.updateLiveValues(devices);
        }));
        liveRefresh.setCycleCount(Timeline.INDEFINITE);
        liveRefresh.play();
    }

    /**
     * Updates UI element visibility based on the current user's permissions for the home.
     *
     * <p>Currently manages the "Add Device" button, which is only shown to users
     * with the {@code canAddDevices} permission. The button is both hidden and
     * removed from layout when not permitted.</p>
     */
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

    /**
     * Updates the room name label from the current session's selected room.
     */
    private void setLabel() {
        String room = Session.getSelectedRoom().getRoomLabel();
        roomLabel.setText(room);
    }

    /**
     * Clears and re-renders all device cards in the flow pane.
     *
     * <p>Permission flags ({@code canDelete}, {@code canConfigure}) are resolved per call
     * and passed to the card factory so each card displays the correct controls.</p>
     */
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

    /**
     * Handles editing a device's label (name).
     *
     * <p>Displays a styled text input dialog pre-filled with the current device name.
     * Validates the new name (not empty, max 100 characters), saves to the database,
     * and re-renders the device list on success.</p>
     *
     * <p>Requires {@code canEditRoomDetails} permission.</p>
     *
     * @param device the device whose label to edit (must not be {@code null})
     */
    private void handleEditDevice(Device device) {
        // CHECK permission
        Room room = Session.getSelectedRoom();
        if (room == null || room.getHome() == null) {
            dialog.error("Permission Denied", "No home context available.");
            return;
        }
        if (!authService.canEditRoomDetails(room.getHome().getId())) {
            dialog.error("Permission Denied",
                    "Only residents and owners can edit device names.");
            return;
        }

        // Create styled dialog for editing device name
        TextInputDialog nameDialog = UIUtils.styledTextInputDialog("Device name:");
        nameDialog.setTitle("Edit Device: " + device.getLabel());
        nameDialog.getEditor().setText(device.getLabel());

        nameDialog.showAndWait().ifPresent(newName -> {
            String trimmedName = newName.trim();

            // Validate name is not empty
            if (trimmedName.isEmpty()) {
                dialog.error("Validation Error", "Device name cannot be empty.");
                return;
            }

            // Validate name length (reasonable limit)
            if (trimmedName.length() > 100) {
                dialog.error("Validation Error", "Device name cannot exceed 100 characters.");
                return;
            }

            // Update device name
            device.setLabel(trimmedName);

            // Save to database
            int updated = deviceRepository.updateDevice(device);

            if (updated > 0) {
                dialog.info("Success", "Device name updated successfully!");
                // Reload devices to ensure consistency
                devices = roomDevicesService.loadDevicesAndRegisterSensors(room);
                room.setDevices(devices);
                renderDevices();
            } else {
                dialog.error("Error", "Failed to update device name. Please try again.");
            }
        });
    }

    /**
     * Handles opening the configuration dialog for an actuator device.
     *
     * <p>Delegates to the appropriate configuration dialog based on actuator type:</p>
     * <ul>
     *   <li>Ventilation → {@link #showVentilationConfig(Device)}</li>
     *   <li>AlarmSystem → {@link #showAlarmConfig(Device)}</li>
     *   <li>Cat Feeder → {@link #showCatFeederConfig(Device)}</li>
     *   <li>Heating → {@link #showHeatingConfig(Device)}</li>
     *   <li>Others → info alert indicating no configuration available</li>
     * </ul>
     *
     * <p>Requires {@code canConfigureActuators} permission.</p>
     *
     * @param actuatorDevice the actuator device to configure (must not be {@code null})
     */
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
        } else if ("Heating".equals(type) || "Heating (%)".equals(type)) {
            showHeatingConfig(actuatorDevice);
        } else {
            UIUtils.styledAlert(Alert.AlertType.INFORMATION, "No configuration available for: " + type, ButtonType.OK).showAndWait();
        }
    }

    /**
     * Handles deleting a device after user confirmation.
     *
     * <p>Displays a confirmation dialog. On confirmation, deletes the device from the
     * database and reloads/re-renders the device list.</p>
     *
     * <p>Requires {@code canRemoveDevices} permission.</p>
     *
     * @param d the device to delete (must not be {@code null})
     */
    private void handleDeleteDevice(Device d) {
        // CHECK permission
        Room room = Session.getSelectedRoom();
        if (room == null || room.getHome() == null) {
            dialog.error("Permission Denied", "No home context available.");
            return;
        }
        if (!authService.canRemoveDevices(room.getHome().getId())) {
            dialog.error("Permission Denied",
                    "Only residents and owners can delete devices.");
            return;
        }

        Alert confirm = UIUtils.styledConfirm("Delete \"" + d.getLabel() + "\"?");
        confirm.setTitle("Delete Device");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                deviceRepository.deleteDevice(d.getId());
                // Reload devices AFTER deletion
                devices = roomDevicesService.loadDevicesAndRegisterSensors(room);
                room.setDevices(devices);
                renderDevices();
            }
        });
    }

    /**
     * Stops the live refresh timeline if it is running.
     *
     * <p>Must be called before navigating away from this view to prevent
     * the timer from continuing to fire after the controller is no longer active.</p>
     */
    private void stopLiveRefresh() {
        if (liveRefresh != null) liveRefresh.stop();
    }

    /**
     * Navigates back to the main dashboard, stopping the live refresh first.
     */
    public void handleDashboard() {
        stopLiveRefresh();
        navigate.goTo(Page.DASHBOARD.fxml());
    }

    /**
     * Handles the Add Device button by presenting category and type selection dialogs.
     *
     * <p>Guides the user through a two-step process:</p>
     * <ol>
     *   <li>Select device category (SENSOR or ACTUATOR)</li>
     *   <li>Select device type from available types of that category</li>
     *   <li>Enter a label/name for the new device</li>
     * </ol>
     *
     * <p>After creation, the device is attached to the appropriate sensor/actuator
     * type table and the device list is reloaded.</p>
     *
     * <p>Requires {@code canAddDevices} permission.</p>
     */
    public void handleAddDevice() {
        Room room = Session.getSelectedRoom();
        if (room == null || room.getHome() == null) {
            dialog.error("Permission Denied", "No home context available.");
            return;
        }
        // CHECK permission
        if (!authService.canAddDevices(room.getHome().getId())) {
            dialog.error("Permission Denied",
                    "Only residents and owners can add devices.");
            return;
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

                    // Reload devices AFTER adding
                    devices = roomDevicesService.loadDevicesAndRegisterSensors(room);
                    room.setDevices(devices);
                    renderDevices();
                });
            });
        });
    }

    /**
     * Navigates to the user profile page, storing the dashboard as the return page.
     */
    public void handleUserProfile() {
        Session.setPreviousPage(Page.DASHBOARD.fxml());
        navigate.goTo(Page.PROFILE.fxml());
    }

    /**
     * Handles logout by showing an info message and navigating to the login page.
     */
    public void handleLogout() {
        dialog.info("Logout", "You have been logged out.");
        navigate.goTo(Page.LOGIN.fxml());
    }

    /**
     * Delegates to {@link ActuatorConfigDialog} to show the ventilation configuration dialog.
     *
     * @param actuatorDevice the ventilation actuator to configure (must not be {@code null})
     */
    private void showVentilationConfig(Device actuatorDevice) {
        actuatorConfigDialog.show(actuatorDevice);
    }

    /**
     * Delegates to {@link ActuatorConfigDialog} to show the alarm system configuration dialog.
     *
     * @param actuatorDevice the alarm system actuator to configure (must not be {@code null})
     */
    private void showAlarmConfig(Device actuatorDevice) {
        actuatorConfigDialog.show(actuatorDevice);
    }

    /**
     * Displays the configuration dialog for cat feeder actuators.
     *
     * <p>Allows configuration of:</p>
     * <ul>
     *   <li>Minimum confidence percentage (0-100%) for cat detection</li>
     *   <li>Cooldown ticks between feedings (0-600 ticks, each tick ≈ 2 seconds)</li>
     * </ul>
     *
     * <p>A hint label shows the cooldown in seconds alongside the tick spinner.</p>
     *
     * @param actuatorDevice the cat feeder actuator to configure (must not be {@code null})
     */
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

    /**
     * Displays the configuration dialog for heating actuators.
     *
     * <p>Allows configuration of:</p>
     * <ul>
     *   <li>Auto mode (enabled/disabled)</li>
     *   <li>Manual heating percentage (0-100%) — disabled in auto mode</li>
     *   <li>Target temperature in °C (10-30°C) — only relevant in auto mode</li>
     *   <li>Hysteresis in °C (0-5°C) — prevents rapid on/off switching</li>
     * </ul>
     *
     * <p>The manual percent spinner is disabled when auto mode is on; target
     * temperature and hysteresis spinners are disabled when auto mode is off.</p>
     *
     * @param actuatorDevice the heating actuator to configure (must not be {@code null})
     */
    private void showHeatingConfig(Device actuatorDevice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configure Heating");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
        );

        var cfg = actuatorCfg.getOrCreateHeatingConfig(actuatorDevice.getId());

        CheckBox autoMode = new CheckBox("Auto mode");
        autoMode.setSelected(cfg.isAutoMode());

        Spinner<Integer> manualPercent = new Spinner<>(0, 100, cfg.getManualPercent());
        manualPercent.setDisable(cfg.isAutoMode());

        Spinner<Double> targetTemp = new Spinner<>(10.0, 30.0, cfg.getTargetTempC(), 0.5);
        Spinner<Double> hysteresis = new Spinner<>(0.0, 5.0, cfg.getHysteresisC(), 0.1);

        // Auto -> Manual UI toggling
        autoMode.selectedProperty().addListener((obs, o, on) -> {
            manualPercent.setDisable(on);
            targetTemp.setDisable(!on);
            hysteresis.setDisable(!on);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(autoMode, 0, 0, 2, 1);

        grid.add(new Label("Manual percent:"), 0, 1);
        grid.add(manualPercent, 1, 1);

        grid.add(new Label("Target temp (°C):"), 0, 2);
        grid.add(targetTemp, 1, 2);

        grid.add(new Label("Hysteresis (°C):"), 0, 3);
        grid.add(hysteresis, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            cfg.setAutoMode(autoMode.isSelected());
            cfg.setManualPercent(manualPercent.getValue());
            cfg.setTargetTempC(targetTemp.getValue());
            cfg.setHysteresisC(hysteresis.getValue());

            actuatorCfg.saveHeatingConfig(actuatorDevice.getId(), cfg);
            UIUtils.styledAlert(Alert.AlertType.INFORMATION, "Saved heating config.", ButtonType.OK).showAndWait();
        });
    }

    // -------------------------------------------------------------------------
    // Getters for testing purposes
    // -------------------------------------------------------------------------

    /**
     * Returns the current list of devices in the room.
     *
     * @return the current device list (never {@code null})
     */
    public List<Device> getDevices() {
        return devices;
    }

    /**
     * Returns the device repository used by this controller.
     *
     * @return the {@link DeviceRepository} instance (never {@code null})
     */
    public DeviceRepository getDeviceRepository() {
        return deviceRepository;
    }

    /**
     * Returns the sensor simulation service used by this controller.
     *
     * @return the {@link SensorSimulationService} instance (never {@code null})
     */
    public SensorSimulationService getSensorSim() {
        return sensorSim;
    }

    /**
     * Replaces the current device list.
     *
     * <p>Intended for testing purposes to inject a controlled device list
     * without loading from the database.</p>
     *
     * @param devices the new device list (must not be {@code null})
     */
    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    /**
     * Returns the actuator service used by this controller.
     *
     * @return the {@link ActuatorService} instance (never {@code null})
     */
    public ActuatorService getActuatorService() {
        return actuatorService;
    }

    /**
     * Returns the actuator configuration service used by this controller.
     *
     * @return the {@link ActuatorConfigService} instance (never {@code null})
     */
    public ActuatorConfigService getActuatorCfg() {
        return actuatorCfg;
    }

    /**
     * Returns the actuator configuration dialog used by this controller.
     *
     * @return the {@link ActuatorConfigDialog} instance (never {@code null})
     */
    public ActuatorConfigDialog getActuatorConfigDialog() {
        return actuatorConfigDialog;
    }
}