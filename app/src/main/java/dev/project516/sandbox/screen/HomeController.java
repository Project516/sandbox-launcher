package dev.project516.sandbox.screen;

import dev.project516.sandbox.core.*;
import dev.project516.sandbox.core.ModLoaderManager;
import dev.project516.sandbox.model.Instance;
import dev.project516.sandbox.model.mojang.VersionInfo;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** Home Menu **/
public class HomeController {

    String osName = System.getProperty("os.name").toLowerCase();

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
    private void initialize() {
        List<Instance> loadedInstances = InstanceManager.loadInstances();
        ObservableList<Instance> observableInstances = FXCollections.observableList(loadedInstances);
        instanceListView.setItems(observableInstances);

        instanceListView.setCellFactory(listView -> new InstanceListCell());

        launchButton.setDisable(false);
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

        // needs x server to launch
        if (!osName.contains("linux") && !isXServerRunning(osName)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("X Server Not Running!");
            alert.setHeaderText("X Server is not running!");
            alert.setContentText("Please download X Server to launch!");
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
                        Process proc = SandboxManager.linuxLaunchInstanceInDocker(
                                selected, consoleController.getOutputConsumer());
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

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("new_instance.fxml"));
            VBox root = loader.load();
            NewInstanceController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("New Instance");
            stage.setScene(new Scene(root));
            controller.setStage(stage);
            stage.initModality(Modality.APPLICATION_MODAL);

            controller.setOnCreateCallback((newInstance, versionUrl) -> {
                isDownloading = true;
                launchButton.setDisable(true);
                addTestButton.setDisable(true);

                instanceListView.getItems().add(newInstance);
                InstanceManager.saveInstances(instanceListView.getItems());
                downloadInstance(newInstance, versionUrl);
            });

            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Download Instance **/
    private void downloadInstance(Instance instance, String versionUrl) {
        Label statusLabel = new Label("Starting download...");
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(-1.0);

        VBox popupVBox = new VBox(15, statusLabel, progressBar);
        popupVBox.setAlignment(Pos.CENTER);
        popupVBox.setPadding(new Insets(20));

        Stage popupStage = new Stage();
        popupStage.setTitle("Downloading " + instance.name());
        popupStage.setScene(new Scene(popupVBox, 300, 100));
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.show();

        new Thread(() -> {
                    Path dest = Path.of(
                            System.getProperty("user.home"),
                            ".sandbox-launcher",
                            "versions",
                            instance.mcVersion(),
                            instance.mcVersion() + ".json");

                    Platform.runLater(() -> statusLabel.setText("Fetching version JSON..."));
                    DownloadManager.downloadFile(versionUrl, dest);

                    Platform.runLater(() -> statusLabel.setText("Downloading client JAR..."));
                    DownloadManager.downloadClientJar(dest, instance.mcVersion(), progress -> {
                        Platform.runLater(() -> progressBar.setProgress(progress));
                    });

                    Platform.runLater(() -> statusLabel.setText("Downloading libraries..."));
                    DownloadManager.downloadLibraries(dest, progress -> {
                        Platform.runLater(() -> progressBar.setProgress(progress));
                    });
                    DownloadManager.extractNatives(dest, instance.mcVersion());

                    try {
                        VersionInfo info = DownloadManager.MAPPER.readValue(dest.toFile(), VersionInfo.class);

                        Platform.runLater(() -> statusLabel.setText("Downloading assets (this may take a bit)..."));
                        DownloadManager.downloadAssets(info, progress -> {
                            Platform.runLater(() -> progressBar.setProgress(progress));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (!instance.modLoader().equalsIgnoreCase("vanilla")) {
                        Platform.runLater(() -> statusLabel.setText("Installing " + instance.modLoader() + "..."));
                        try {
                            ModLoaderManager.install(instance, progress -> {
                                Platform.runLater(() -> progressBar.setProgress(progress));
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            Platform.runLater(() -> new Alert(
                                            Alert.AlertType.ERROR,
                                            "Failed to install " + instance.modLoader() + ":\n" + e.getMessage())
                                    .showAndWait());
                        }
                    }

                    Platform.runLater(() -> {
                        isDownloading = false;
                        launchButton.setDisable(false);
                        addTestButton.setDisable(false);
                        popupStage.close();
                        System.out.println("[UI] Download Complete");
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

    // @FXML ?
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

    // @FXML ?
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

    @FXML
    public void onSetIconClick() {
        Instance selected = instanceListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No Instance Selected!");
            alert.setContentText("Please select an instance to change its icon!");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Instance Icon");
        fileChooser
                .getExtensionFilters()
                .addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile =
                fileChooser.showOpenDialog(instanceListView.getScene().getWindow());

        if (selectedFile != null) {
            try {
                Path iconsDir = Path.of(System.getProperty("user.home"), ".sandbox-launcher", "icons");
                Files.createDirectories(iconsDir);

                String safeFileName = selected.name().replaceAll("[^a-zA-Z0-9]", "_") + ".png";
                Path destPath = iconsDir.resolve(safeFileName);
                Files.copy(selectedFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

                int index = instanceListView.getItems().indexOf(selected);
                Instance updatedInstance = new Instance(selected.name(), selected.mcVersion(), destPath.toString());

                instanceListView.getItems().set(index, updatedInstance);
                InstanceManager.saveInstances(instanceListView.getItems());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void onEditClick() {
        Instance selected = instanceListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No Instance Selected!");
            alert.setContentText("Please select an instance to edit!");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("instance_settings.fxml"));
            VBox root = loader.load();
            InstanceSettingsController settingsController = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Settings - " + selected.name());
            settingsController.setStage(stage);
            settingsController.setInstance(selected);

            settingsController.setOnSaveCallback(updateInstance -> {
                int index = instanceListView.getItems().indexOf(selected);
                instanceListView.getItems().set(index, updateInstance);
                InstanceManager.saveInstances(instanceListView.getItems());
            });

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Detect if xserver is running (needed to launch the game) **/
    private boolean isXServerRunning(String osName) {

        if (osName.contains("win")) {
            return ProcessHandle.allProcesses().anyMatch(process -> {
                String command = process.info().command().orElse("").toLowerCase();
                return command.contains("vcxsrv");
            });

        } else if (osName.contains("mac")) {
            return ProcessHandle.allProcesses().anyMatch(process -> {
                String command = process.info().command().orElse("").toLowerCase();
                return command.contains("xquartz") || command.contains("x11");
            });
        } else {
            return false;
        }
    }
}
