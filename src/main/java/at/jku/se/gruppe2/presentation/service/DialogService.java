package at.jku.se.gruppe2.presentation.service;

import at.jku.se.gruppe2.presentation.util.UIUtils;
import javafx.scene.control.*;

import java.util.Optional;

/**
 * Service for displaying styled modal dialogs throughout the application.
 *
 * <p>This service provides a thin, consistent abstraction over JavaFX {@link Alert}
 * dialogs, ensuring all dialogs are styled uniformly via the global application
 * stylesheet. It centralizes common dialog patterns to reduce boilerplate in
 * controller classes.</p>
 *
 * <p><b>Supported dialog types:</b></p>
 * <ul>
 *   <li><b>Info:</b> Informational messages (e.g., success confirmations)</li>
 *   <li><b>Error:</b> Error messages (e.g., validation failures, permission denials)</li>
 *   <li><b>Confirm:</b> Yes/No confirmation prompts (e.g., delete confirmations)</li>
 * </ul>
 *
 * <p>All dialogs block the calling thread until dismissed ({@code showAndWait}),
 * as they are intended for synchronous user interaction flows.</p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * DialogService dialog = new DialogService();
 *
 * // Simple info
 * dialog.info("Success", "Room created successfully!");
 *
 * // Conditional confirmation
 * dialog.confirm("Delete?", "Are you sure?").ifPresent(btn -> {
 *     if (btn == ButtonType.OK) deleteItem();
 * });
 * }</pre>
 *
 * @see UIUtils#styledAlert(Alert.AlertType, String, ButtonType...)
 */
public class DialogService {

    /**
     * Displays an informational dialog with a single OK button.
     *
     * <p>This is a convenience overload of {@link #info(String, String, ButtonType)}
     * that uses {@link ButtonType#OK} as the default button type.</p>
     *
     * @param title the title of the dialog window (must not be {@code null})
     * @param message the message to display in the dialog body (must not be {@code null})
     */
    public void info(String title, String message) {
        info(title, message, ButtonType.OK);
    }

    /**
     * Displays an informational dialog with the specified button type.
     *
     * <p>The dialog is styled with the application stylesheet, has no header text
     * or graphic, and blocks until the user dismisses it.</p>
     *
     * @param title the title of the dialog window (must not be {@code null})
     * @param message the message to display in the dialog body (must not be {@code null})
     * @param buttonType the button type to present in the dialog (must not be {@code null})
     */
    public void info(String title, String message, ButtonType buttonType) {
        Alert alert = UIUtils.styledAlert(
                Alert.AlertType.INFORMATION,
                message,
                buttonType
        );
        alert.setTitle(title);
        alert.showAndWait();
    }

    /**
     * Displays an error dialog with a single OK button.
     *
     * <p>This is a convenience overload of {@link #error(String, String, ButtonType)}
     * that uses {@link ButtonType#OK} as the default button type.</p>
     *
     * @param title the title of the dialog window (must not be {@code null})
     * @param message the error message to display in the dialog body (must not be {@code null})
     */
    public void error(String title, String message) {
        error(title, message, ButtonType.OK);
    }

    /**
     * Displays an error dialog with the specified button type.
     *
     * <p>The dialog is styled with the application stylesheet, has no header text
     * or graphic, and blocks until the user dismisses it.</p>
     *
     * @param title the title of the dialog window (must not be {@code null})
     * @param message the error message to display in the dialog body (must not be {@code null})
     * @param buttonType the button type to present in the dialog (must not be {@code null})
     */
    public void error(String title, String message, ButtonType buttonType) {
        Alert alert = UIUtils.styledAlert(
                Alert.AlertType.ERROR,
                message,
                buttonType
        );
        alert.setTitle(title);
        alert.showAndWait();
    }

    /**
     * Displays a confirmation dialog with OK and Cancel buttons.
     *
     * <p>The dialog is styled with the application stylesheet and blocks until
     * the user makes a selection. The returned {@link Optional} contains the
     * button the user clicked, or is empty if the dialog was closed without
     * a selection.</p>
     *
     * @param title the title of the dialog window (must not be {@code null})
     * @param message the confirmation question to display (must not be {@code null})
     * @return an {@link Optional} containing the clicked {@link ButtonType},
     *         or {@link Optional#empty()} if the dialog was dismissed
     */
    public Optional<ButtonType> confirm(String title, String message) {
        Alert alert = UIUtils.styledAlert(
                Alert.AlertType.CONFIRMATION,
                message,
                ButtonType.OK,
                ButtonType.CANCEL
        );
        alert.setTitle(title);
        return alert.showAndWait();
    }
}