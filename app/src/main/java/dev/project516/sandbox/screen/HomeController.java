package dev.project516.sandbox.screen;

import dev.project516.sandbox.core.InstanceManager;
import dev.project516.sandbox.core.MojangManager;
import dev.project516.sandbox.core.SandboxManager;
import dev.project516.sandbox.model.Instance;
import dev.project516.sandbox.model.mojang.Version;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;

/** Home Menu **/
public class HomeController {

    @FXML
    private Button launchButton;

    @FXML
    private Button quitButton;

    @FXML
    private ListView<Instance> instanceListView;

    @FXML
    private Button addTestButton;

    @FXML
    private Button deleteTestButton;

    @FXML
    private ComboBox<Version> versionComboBox;

    @FXML
    private void initialize() {
        List<Instance> loadedInstances = InstanceManager.loadInstances();
        ObservableList<Instance> observableInstances = FXCollections.observableList(loadedInstances);
        instanceListView.setItems(observableInstances);

        MojangManager.fetchVersionManifest()
                .thenAccept(manifest -> {
                    if (manifest != null) {
                        Platform.runLater(() -> {
                            versionComboBox.getItems().setAll(manifest.versions());
                        });
                    }
                })
                .exceptionally(e -> {
                    System.err.println("Failed to fetch versions!");
                    e.printStackTrace();
                    return null;
                });
    }

    @FXML
    private void onLaunchClick() {
        // System.out.println("Launch clicked");

        Instance selected = instanceListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No Instance Selected!");
            alert.setContentText("Please select an Instance");
            alert.showAndWait();
            return;
        }

        System.out.println("Launching: : " + selected.name());

        new Thread(() -> {
                    SandboxManager.startPersistentContainer();
                })
                .start();
    }

    @FXML
    private void onQuitClick() {
        System.out.println("Quit clicked");
        Platform.exit();
    }

    @FXML
    private void onAddClick() {
        Version selectedVersion = versionComboBox.getSelectionModel().getSelectedItem();
        if (selectedVersion == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No Version Selected!");
            alert.setContentText("Please select a Version");
            alert.showAndWait();
            return;
        }

        Instance newInstance = new Instance("New Instance", selectedVersion.id());

        instanceListView.getItems().add(newInstance);
        InstanceManager.saveInstances(instanceListView.getItems());
    }

    @FXML
    private void onDeleteClick() {
        Instance selected = instanceListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No Instance Selected!");
            alert.setContentText("Please select an instance!");
            alert.showAndWait();
            return;
        }
        instanceListView.getItems().remove(selected);
        InstanceManager.saveInstances(instanceListView.getItems());
    }
}
