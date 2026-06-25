package dev.project516.sandbox.screen;

import dev.project516.sandbox.core.DownloadManager;
import dev.project516.sandbox.core.InstanceManager;
import dev.project516.sandbox.core.MojangManager;
import dev.project516.sandbox.core.SandboxManager;
import dev.project516.sandbox.model.Instance;
import dev.project516.sandbox.model.mojang.Version;
import dev.project516.sandbox.model.mojang.VersionInfo;
import java.nio.file.Path;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Home Menu **/
public class HomeController {

    private boolean isDownloading = false;

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

        instanceListView.setCellFactory(listView -> new ListCell<Instance>() {
            private final ImageView iconView = new ImageView();
            private final Label nameLabel = new Label();
            private final Label versionLabel = new Label();
            private final VBox textBox = new VBox(nameLabel, versionLabel);
            private final HBox layout = new HBox(10, iconView, textBox);

            {
                iconView.setFitWidth(32);
                iconView.setFitHeight(32);

                try {
                    Image icon = new Image(getClass().getResource("icon.png").toExternalForm());
                    iconView.setImage(icon);
                } catch (Exception e) {
                    System.err.println("Could not load icon.png");
                }

                versionLabel.setStyle("-fx-text-fill: derive(-fx-text-background-color, -30%); -fx-font-size: 11px;");

                layout.setPadding(new Insets(5, 5, 5, 5));
                layout.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Instance instance, boolean empty) {
                super.updateItem(instance, empty);

                if (empty || instance == null) {
                    setGraphic(null);
                } else {
                    nameLabel.setText(instance.name());
                    versionLabel.setText("Minecraft " + instance.mcVersion());
                    setGraphic(layout);
                }
            }
        });

        MojangManager.fetchVersionManifest()
                .thenAccept(manifest -> {
                    if (manifest != null) {
                        Platform.runLater(() -> {
                            List<Version> releasesOnly = manifest.versions().stream()
                                    .filter(version -> version.type().equalsIgnoreCase("release"))
                                    .toList();
                            versionComboBox.getItems().setAll(releasesOnly);
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
                    SandboxManager.launchInstanceInDocker(selected.mcVersion());
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
        if (isDownloading) return;
        isDownloading = true;
        launchButton.setDisable(true);
        addTestButton.setDisable(true);

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

        new Thread(() -> {
                    Path dest = Path.of(
                            System.getProperty("user.home"),
                            ".sandbox-launcher",
                            "versions",
                            selectedVersion.id(),
                            selectedVersion.id() + ".json");

                    DownloadManager.downloadFile(selectedVersion.url(), dest);
                    DownloadManager.downloadClientJar(dest, selectedVersion.id());
                    DownloadManager.downloadLibraries(dest);
                    DownloadManager.extractNatives(dest, selectedVersion.id());

                    try {
                        VersionInfo info = DownloadManager.MAPPER.readValue(dest.toFile(), VersionInfo.class);
                        DownloadManager.downloadAssets(info);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        isDownloading = false;
                        launchButton.setDisable(false);
                        addTestButton.setDisable(false);
                        System.out.println("[UI] Download complete!");
                    });
                })
                .start();
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

        InstanceManager.deleteVersionFiles(selected.mcVersion());

        instanceListView.getItems().remove(selected);
        InstanceManager.saveInstances(instanceListView.getItems());
    }

    @FXML
    private void usernameBar() {
        // TODO make a bar for setting/editing username
    }
}
