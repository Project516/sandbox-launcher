package dev.project516.sandbox.screen;

import dev.project516.sandbox.core.FabricManager;
import dev.project516.sandbox.core.ForgeManager;
import dev.project516.sandbox.core.MojangManager;
import dev.project516.sandbox.core.NeoForgeManager;
import dev.project516.sandbox.model.Instance;
import dev.project516.sandbox.model.mojang.Version;
import dev.project516.sandbox.model.mojang.VersionManifest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/** Select new Instance menu **/
public class NewInstanceController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<Version> versionComboBox;

    @FXML
    private ComboBox<String> modLoaderCombo;

    @FXML
    private Button createButton;

    @FXML
    private Button cancelButton;

    private Stage stage;
    private BiConsumer<Instance, String> onCreateCallback;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnCreateCallback(BiConsumer<Instance, String> onCreateCallback) {
        this.onCreateCallback = onCreateCallback;
    }

    @FXML
    private void initialize() {

        modLoaderCombo.setDisable(true);
        modLoaderCombo.setItems(FXCollections.observableArrayList("vanilla"));
        modLoaderCombo.setValue("vanilla");

        versionComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                checkLoaderSupport(newVal.id());
            }
        });

        VersionManifest localManifest = MojangManager.loadLocalManifest();
        if (localManifest != null) {
            List<Version> releases = localManifest.versions().stream()
                    .filter(v -> v.type().equalsIgnoreCase("release"))
                    .toList();
            versionComboBox.getItems().addAll(releases);
        }

        MojangManager.fetchVersionManifest()
                .thenAccept(manifest -> {
                    if (manifest != null) {
                        javafx.application.Platform.runLater(() -> {
                            List<Version> releases = manifest.versions().stream()
                                    .filter(v -> v.type().equalsIgnoreCase("release"))
                                    .toList();
                            versionComboBox.getItems().setAll(releases);
                            if (!releases.isEmpty()) {
                                versionComboBox.getSelectionModel().select(0);
                            }
                        });
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    private void checkLoaderSupport(String mcVersion) {
        modLoaderCombo.setDisable(true);
        modLoaderCombo.setItems(FXCollections.observableArrayList("vanilla"));
        modLoaderCombo.setValue("vanilla");

        new Thread(() -> {
                    boolean fabricSupported = false;
                    boolean forgeSupported = false;
                    boolean neoForgeSupported = false;

                    try {
                        fabricSupported = FabricManager.fetchLatestLoader(mcVersion) != null;
                    } catch (Exception ignored) {
                    }

                    try {
                        forgeSupported = ForgeManager.latestVersion(mcVersion) != null;
                    } catch (Exception ignored) {
                    }

                    try {
                        neoForgeSupported = NeoForgeManager.latestVersion(mcVersion) != null;
                    } catch (Exception Ignored) {
                    }
                    ;

                    List<String> supported = new ArrayList<>();
                    supported.add("vanilla");
                    if (fabricSupported) supported.add("fabric");
                    if (forgeSupported) supported.add("forge");
                    if (neoForgeSupported) supported.add("neoforge");

                    javafx.application.Platform.runLater(() -> {
                        modLoaderCombo.setItems(FXCollections.observableArrayList(supported));
                        modLoaderCombo.setValue("vanilla");
                        modLoaderCombo.setDisable(false);
                    });
                })
                .start();
    }

    @FXML
    private void onCreateClick() {
        Version selectedVersion = versionComboBox.getSelectionModel().getSelectedItem();
        if (selectedVersion == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a version").showAndWait();
            return;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            name = selectedVersion.id() + "Instance";
        }

        String loader = modLoaderCombo.getValue() != null ? modLoaderCombo.getValue() : "vanilla";

        Instance newInstance = new Instance(name, selectedVersion.id(), null, loader);
        if (onCreateCallback != null) {
            onCreateCallback.accept(newInstance, selectedVersion.url());
        }
        stage.close();
    }

    @FXML
    private void onCancelClick() {
        stage.close();
    }
}
