package at.jku.se.gruppe2.presentation.component.custom;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
 * Custom text field component that accepts only integer input.
 *
 * <p>This component extends {@link TextField} and restricts user input to
 * digits only, automatically parsing and storing the value as an integer.
 * Invalid characters are rejected immediately during typing.</p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Accepts only numeric digit input (0-9)</li>
 *   <li>Maintains an observable {@link IntegerProperty} for data binding</li>
 *   <li>Treats empty field as value 0</li>
 *   <li>Rejects non-numeric and unparseable input</li>
 * </ul>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * IntegerField ageField = new IntegerField();
 * ageField.setValue(25);
 * ageField.valueProperty().addListener((obs, oldVal, newVal) -> {
 *     System.out.println("New age: " + newVal);
 * });
 * }</pre>
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Does not support negative numbers</li>
 *   <li>Does not handle integer overflow (values beyond {@link Integer#MAX_VALUE})</li>
 * </ul>
 */
public class IntegerField extends TextField {

    /**
     * Observable property holding the current integer value of this field.
     *
     * <p>This property is automatically updated when the text changes and
     * can be used for bidirectional data binding in JavaFX applications.</p>
     */
    private final IntegerProperty value = new SimpleIntegerProperty(this, "value");

    /**
     * Constructs a new integer-only text field.
     *
     * <p>The field is initialized with a {@link TextFormatter} that:</p>
     * <ul>
     *   <li>Accepts only digit characters (0-9)</li>
     *   <li>Rejects any non-numeric input</li>
     *   <li>Parses valid input and updates the {@code value} property</li>
     *   <li>Treats empty input as value 0</li>
     * </ul>
     *
     * <p>The formatter prevents invalid text from ever appearing in the field,
     * providing immediate feedback to the user.</p>
     */
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

    /**
     * Returns the current integer value of this field.
     *
     * <p>This is equivalent to calling {@code valueProperty().get()}.</p>
     *
     * @return the current integer value (0 if field is empty)
     */
    public int getValue() {
        return value.get();
    }

    /**
     * Sets the integer value of this field.
     *
     * <p>This updates both the internal {@code value} property and the
     * displayed text. The text is set to the string representation of
     * the provided value.</p>
     *
     * @param value the new integer value to set
     */
    public void setValue(int value) {
        setText(String.valueOf(value));
    }
}