package at.jku.se.gruppe2.presentation.component.custom;

import at.jku.se.gruppe2.domain.model.home.Home;
import at.jku.se.gruppe2.domain.service.room.RoomService;
import at.jku.se.gruppe2.presentation.util.UIUtils;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class CreateRoomDialog extends Dialog<Void> {

    private final TextField nameField = new TextField();
    private final ComboBox<Integer> floorBox = new ComboBox<>();
    private final TextField lengthField = new TextField();
    private final TextField widthField = new TextField();

    private final Label areaLabel = new Label("Area: -");
    private final Label errorLabel = new Label();

    public CreateRoomDialog(Home home, RoomService roomService) {

        setTitle("Create Room");
        setHeaderText(null);
        setGraphic(null);

        // Apply global stylesheet
        getDialogPane().getStylesheets().add(
                UIUtils.class.getResource("/css/app.css").toExternalForm()
        );

        ButtonType createType =
                new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        configureFields(home);
        configureAreaCalculation();
        configureValidation(createType);

        VBox card = new VBox(12,
                buildForm(),
                areaLabel,
                errorLabel
        );
        card.getStyleClass().add("card");

        getDialogPane().setContent(card);

        setResultConverter(btn -> {
            if (btn == createType) {
                roomService.createRoom(
                        nameField.getText().trim(),
                        home,
                        floorBox.getValue(),
                        parseOptionalPositive(lengthField.getText()),
                        parseOptionalPositive(widthField.getText())
                );
            }
            return null;
        });
    }

    private void configureFields(Home home) {
        nameField.setPromptText("e.g. Living Room");

        floorBox.setPromptText("Select floor");
        for (int i = 1; i <= home.getFloors(); i++) {
            floorBox.getItems().add(i);
        }

        lengthField.setPromptText("optional");
        widthField.setPromptText("optional");

        areaLabel.getStyleClass().add("muted");

        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
    }

    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Room name"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Floor"), 0, 1);
        grid.add(floorBox, 1, 1);

        grid.add(new Label("Length (m)"), 0, 2);
        grid.add(lengthField, 1, 2);

        grid.add(new Label("Width (m)"), 0, 3);
        grid.add(widthField, 1, 3);

        return grid;
    }

    private void configureAreaCalculation() {
        Runnable update = () -> {
            try {
                Double l = parseOptionalPositive(lengthField.getText());
                Double w = parseOptionalPositive(widthField.getText());

                if (l != null && w != null) {
                    areaLabel.setText("Area: " + (double)Math.round((l*w) * 100d) / 100d + " m²");
                } else {
                    areaLabel.setText("Area: -");
                }
                errorLabel.setVisible(false);
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }
        };

        lengthField.textProperty().addListener((a, b, c) -> update.run());
        widthField.textProperty().addListener((a, b, c) -> update.run());
    }

    private void configureValidation(ButtonType createType) {
        Node createBtn = getDialogPane().lookupButton(createType);
        createBtn.setDisable(true);

        Runnable validate = () -> {
            boolean invalid =
                    nameField.getText().isBlank()
                            || floorBox.getValue() == null;

            createBtn.setDisable(invalid);
        };

        nameField.textProperty().addListener((a, b, c) -> validate.run());
        floorBox.valueProperty().addListener((a, b, c) -> validate.run());
    }

    private Double parseOptionalPositive(String value) {
        if (value == null || value.isBlank()) return null;

        String normalized = value.trim().replace(',', '.');

        double d;
        try {
            d = Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Dimensions must be numbers");
        }

        if (d <= 0) {
            throw new IllegalArgumentException("Dimensions must be positive");
        }
        return d;
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}

