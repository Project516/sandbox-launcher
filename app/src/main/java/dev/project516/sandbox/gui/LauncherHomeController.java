package dev.project516.sandbox.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class LauncherHomeController {

    @FXML
    private Label statusLabel;

    @FXML
    private void onButtonClick() {
        statusLabel.setText("Clicked!");
    }
}
