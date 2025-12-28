package at.jku.se.gruppe2.ui.controller;

import at.jku.se.gruppe2.model.*;
import at.jku.se.gruppe2.persistence.*;
import at.jku.se.gruppe2.service.*;
import at.jku.se.gruppe2.ui.UIUtils;
import at.jku.se.gruppe2.ui.component.RoomCardFactory;
import at.jku.se.gruppe2.ui.custom.CreateRoomDialog;
import at.jku.se.gruppe2.ui.navigation.Page;
import at.jku.se.gruppe2.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

public class HomeDashboardController extends BaseController{

    @FXML
    private FlowPane cardsFlow;

    private Home home;
    private Optional<List<Room>> rooms;

    private final HomeRepository homeRepo = new HomeRepository();
    private final RoomRepository roomRepo = new RoomRepository();
    private final DeviceRepository deviceRepo = new DeviceRepository();
    private final RoomService roomService = new RoomService();
    private final RoomCardFactory cardFactory = new RoomCardFactory();

    @FXML
    public void initialize() {
        int userId = Session.getCurrentUser().getId();
        Optional<Home> homeOptional = homeRepo.getHomeByUserId(userId);

        if (homeOptional.isEmpty()) {
            redirectToHomeRegistration();
            return;
        }

        home = homeOptional.get();
        reload();
    }

    private void loadRooms() {
        this.rooms = Optional.of(roomService.loadRoomsWithDevices(home));
    }

    private void renderCards() {
        cardsFlow.getChildren().clear();
        rooms.orElse(Collections.emptyList())
                .forEach(room ->
                        cardsFlow.getChildren().add(
                                cardFactory.createRoomCard(
                                        room,
                                        this::handleManageRoom,
                                        this::handleDeleteRoom
                                )
                        )
                );
    }

    public void handleCreateRoom() {
        CreateRoomDialog dialog =
                new CreateRoomDialog(home, roomService);

        dialog.showAndWait();
        reload();
    }

    public void handleDeleteRoom(Room room) {
        Alert confirm = UIUtils.styledConfirm(
                "Delete \"" + room.getRoomLabel() + "\"?\nAll devices in this room will also be deleted."
        );
        confirm.setTitle("Delete Room");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {

                // deletes devices first ? Is our DB cascading?
                for (Device device : room.getDevices()) {
                    deviceRepo.deleteDevice(device.getId());
                }

                roomRepo.deleteRoom(room.getId());

                reload();

            }
        });

    }

    public void handleUserProfile() {
        handleUserProfile(Page.HOME_DASHBOARD.fxml());
    }

    public void handleManageRoom(Room room) {
        Session.setSelectedRoom(room);
        navigate.goTo(Page.ROOM_DASHBOARD.fxml());
    }

    private void reload() {
        loadRooms();
        renderCards();
    }
}
