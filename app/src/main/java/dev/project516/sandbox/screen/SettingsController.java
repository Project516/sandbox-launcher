package dev.project516.sandbox.screen;

import dev.project516.sandbox.core.PlayerManager;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/** Settings meny **/
public class SettingsController {

    @FXML
    private TextField usernameField;

    @FXML
    private void initialize() {
        usernameField.setText(PlayerManager.getUsername());
    }

    @FXML
    private void onSaveClick() {
        String newName = usernameField.getText().trim();
        if (!newName.isEmpty()) {
            PlayerManager.saveSettings(newName);
        }

        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onCancelClick() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }
}
