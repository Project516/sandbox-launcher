package dev.project516.sandbox.screen;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

public class HomeController {

    @FXML
    private Button launchButton;

    @FXML
    private Button quitButton;

    @FXML
    private ListView<String> instanceListView;

    @FXML
    private void initialize() {
        ObservableList<String> dummyInstances =
                FXCollections.observableArrayList("Vanilla 1.20.4", "All The Mods 10", "Beta 1.7.3");

        instanceListView.setItems(dummyInstances);
    }

    @FXML
    private void onLaunchClick() {
        System.out.println("Launch clicked");

        String selected = instanceListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("Please select an instance!"); // TODO: print warning in app
        }

        InstanceWindow instanceWindow = new InstanceWindow();
        instanceWindow.show();
    }

    @FXML
    private void onQuitClick() {
        System.out.println("Quit clicked");
        Platform.exit();
    }
}
