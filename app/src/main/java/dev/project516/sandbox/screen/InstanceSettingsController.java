package dev.project516.sandbox.screen;

import dev.project516.sandbox.core.FabricManager;
import dev.project516.sandbox.model.Instance;
import dev.project516.sandbox.model.fabric.FabricVersionInfo;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class InstanceSettingsController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> modLoaderCombo;

    @FXML
    private Button installButton;

    private Instance instance;
    private Consumer<Instance> onSaveCallback;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
        nameField.setText(instance.name());
        modLoaderCombo.setValue(instance.modLoader() != null ? instance.modLoader() : "vanilla");
    }

    public void setOnSaveCallback(Consumer<Instance> onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    @FXML
    private void initialize() {
        modLoaderCombo.setItems(FXCollections.observableArrayList("vanilla", "fabric", "forge", "neoforge"));
    }

    @FXML
    private void onInstallClick() {
        String selectedLoader = modLoaderCombo.getValue();

        if (selectedLoader == null || selectedLoader.equalsIgnoreCase("vanilla")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Vanilla");
            alert.setHeaderText("No installation required for Vanilla.");
            alert.showAndWait();
            return;
        }

        installButton.setDisable(true);

        new Thread(() -> {
                    try {
                        if (selectedLoader.equalsIgnoreCase("fabric")) {
                            System.out.println("[FABRIC] Fetching loader for " + instance.mcVersion());
                            FabricVersionInfo fabricInfo = FabricManager.fetchLatestLoader(instance.mcVersion());

                            if (fabricInfo != null) {
                                FabricManager.downloadFabricLibraries(fabricInfo);
                                Platform.runLater(() -> {
                                    instance = new Instance(
                                            instance.name(), instance.mcVersion(), instance.iconPath(), "fabric");
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle("Success");
                                    alert.setHeaderText("Fabric installed successfully!");
                                    alert.showAndWait();
                                });
                            }
                        } else if (selectedLoader.equalsIgnoreCase("forge")
                                || selectedLoader.equalsIgnoreCase("neoforge")) {
                            // TODO: implement forge and neoforge
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.WARNING);
                                alert.setTitle("Not Implemented!");
                                alert.setHeaderText(selectedLoader.toLowerCase() + " installation is coming soon!");
                                alert.showAndWait();
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        Platform.runLater(() -> installButton.setDisable(false));
                    }
                })
                .start();
    }

    @FXML
    private void onSaveClick() {
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            newName = "Unnamed Instance";
        }

        String currentLoader = modLoaderCombo.getValue();
        String finalLoader =
                (instance.modLoader() != null && !instance.modLoader().equals("vanilla"))
                        ? instance.modLoader()
                        : currentLoader;

        Instance updatedInstance = new Instance(newName, instance.mcVersion(), instance.iconPath(), finalLoader);

        if (onSaveCallback != null) {
            onSaveCallback.accept(updatedInstance);
        }

        stage.close();
    }

    @FXML
    private void onCancelClick() {
        stage.close();
    }
}
