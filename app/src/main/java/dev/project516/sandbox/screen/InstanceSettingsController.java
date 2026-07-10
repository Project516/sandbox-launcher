package dev.project516.sandbox.screen;

// import dev.project516.sandbox.core.FabricManager;
import dev.project516.sandbox.core.ModLoaderManager;
import dev.project516.sandbox.model.Instance;
import java.util.function.Consumer;
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
        String selected = modLoaderCombo.getValue();
        if (selected == null || selected.equalsIgnoreCase("vanilla")) {
            new Alert(Alert.AlertType.INFORMATION, "Vanilla needs no installation.").showAndWait();
            return;
        }
        installButton.setDisable(true);

        final String loader = selected;
        new Thread(() -> {
                    try {

                        Instance target =
                                new Instance(instance.name(), instance.mcVersion(), instance.iconPath(), loader);

                        ModLoaderManager.install(target, p -> {
                            // TODO: progress
                        });

                        instance = target;
                        javafx.application.Platform.runLater(() -> {
                            if (onSaveCallback != null) onSaveCallback.accept(target);
                            new Alert(Alert.AlertType.INFORMATION, loader + " instance successfully").showAndWait();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        javafx.application.Platform.runLater(() -> new Alert(
                                        Alert.AlertType.ERROR, "Failed to install " + loader + ":\n" + e.getMessage())
                                .showAndWait());
                    } finally {
                        javafx.application.Platform.runLater(() -> installButton.setDisable(false));
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

        String loader = modLoaderCombo.getValue() != null ? modLoaderCombo.getValue() : "vanilla";
        Instance updatedInstance = new Instance(newName, instance.mcVersion(), instance.iconPath(), loader);

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
