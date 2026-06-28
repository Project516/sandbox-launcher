package dev.project516.sandbox.screen;

import dev.project516.sandbox.core.DownloadManager;
import dev.project516.sandbox.core.InstanceManager;
import dev.project516.sandbox.core.MojangManager;
import dev.project516.sandbox.core.SandboxManager;
import dev.project516.sandbox.model.Instance;
import dev.project516.sandbox.model.mojang.Version;
import dev.project516.sandbox.model.mojang.VersionInfo;
import dev.project516.sandbox.model.mojang.VersionManifest;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** Home Menu **/
public class HomeController {

    @FXML
    public Button settingsButton;

    @FXML
    public Button renameButton;

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

        instanceListView.setCellFactory(listView -> new InstanceListCell());

        VersionManifest localManifest = MojangManager.loadLocalManifest();
        if (localManifest != null) {
            List<Version> releaseOnly = localManifest.versions().stream()
                    .filter(v -> v.type().equalsIgnoreCase("release"))
                    .toList();
            versionComboBox.getItems().addAll(releaseOnly);
        }

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

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("console.fxml"));
            VBox root = loader.load();
            ConsoleController consoleController = loader.getController();

            Stage consoleStage = new Stage();
            consoleStage.setTitle("Console - " + selected.name());
            consoleStage.setScene(new Scene(root));
            consoleController.setStage(consoleStage);
            consoleStage.show();

            new Thread(() -> {
                        Process proc = SandboxManager.launchInstanceInDocker(
                                selected.mcVersion(), consoleController.getOutputConsumer());
                        Platform.runLater(() -> consoleController.setProcess(proc));
                    })
                    .start();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        TextInputDialog dialog = new TextInputDialog("New Instance");
        dialog.setTitle("New Instance");
        dialog.setHeaderText("Creating instance for " + selectedVersion.id());
        dialog.setContentText("Enter instance name:");
        Optional<String> result = dialog.showAndWait();
        String name = result.filter(s -> !s.trim().isEmpty()).orElse("New Instance");

        Instance newInstance = new Instance(name, selectedVersion.id());
        instanceListView.getItems().add(newInstance);
        InstanceManager.saveInstances(instanceListView.getItems());

        Label statusLabel = new Label("Starting download...");
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(-1.0);

        VBox popupVBox = new VBox(15, statusLabel, progressBar);
        popupVBox.setAlignment(Pos.CENTER);
        popupVBox.setPadding(new Insets(20));

        Stage popupStage = new Stage();
        popupStage.setTitle("Downloading " + name);
        popupStage.setScene(new Scene(popupVBox, 300, 100));
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.show();

        new Thread(() -> {
                    Path dest = Path.of(
                            System.getProperty("user.home"),
                            ".sandbox-launcher",
                            "versions",
                            selectedVersion.id(),
                            selectedVersion.id() + ".json");

                    Platform.runLater(() -> statusLabel.setText("Fetching version JSON..."));
                    DownloadManager.downloadFile(selectedVersion.url(), dest);

                    Platform.runLater(() -> statusLabel.setText("Downloading client JAR..."));
                    DownloadManager.downloadClientJar(dest, selectedVersion.id());

                    Platform.runLater(() -> statusLabel.setText("Downloading libraries..."));
                    DownloadManager.downloadLibraries(dest);
                    DownloadManager.extractNatives(dest, selectedVersion.id());

                    try {
                        VersionInfo info = DownloadManager.MAPPER.readValue(dest.toFile(), VersionInfo.class);

                        Platform.runLater(() -> statusLabel.setText("Downloading assets (this may take a while)..."));
                        DownloadManager.downloadAssets(info);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        isDownloading = false;
                        launchButton.setDisable(false);
                        addTestButton.setDisable(false);
                        popupStage.close();
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

    public void onSettingsClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("settings.fxml"));
            VBox root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onRenameClick() {
        Instance selected = instanceListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No Instance Selected!");
            alert.setContentText("Please select an instance to rename!");
            alert.showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.name());
        dialog.setTitle("Rename Instance");
        dialog.setHeaderText("Renaming " + selected.name());
        dialog.setContentText("Enter new name:");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                int index = instanceListView.getItems().indexOf(selected);

                Instance renamed = new Instance(newName.trim(), selected.mcVersion());

                instanceListView.getItems().set(index, renamed);

                InstanceManager.saveInstances(instanceListView.getItems());
            }
        });
    }
}
