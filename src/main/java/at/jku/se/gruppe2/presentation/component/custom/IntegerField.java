package at.jku.se.gruppe2.presentation.component.custom;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

public class IntegerField extends TextField {

    private final IntegerProperty value = new SimpleIntegerProperty(this, "value");

    public IntegerField() {
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();

            // Allow empty field
            if (newText.isEmpty()) {
                value.set(0);
                return change;
            }

            // Keep only digits
            if (!newText.matches("\\d+")) {
                return null;
            }

            try {
                int intValue = Integer.parseInt(newText);
                value.set(intValue);
            } catch (NumberFormatException ex) {
                return null;
            }

            return change;
        });

        setTextFormatter(formatter);
    }

    public IntegerProperty valueProperty() {
        return value;
    }

    public int getValue() {
        return value.get();
    }

    public void setValue(int value) {
        setText(String.valueOf(value));
    }
}
