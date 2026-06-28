package dev.project516.sandbox.screen;

import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ConsoleController {

    @FXML
    private TextArea consoleArea;

    @FXML
    private Button killButton;

    private Process process;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public Consumer<String> getOutputConsumer() {
        return line -> Platform.runLater(() -> {
            consoleArea.appendText(line + "\n");
            consoleArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    @FXML
    private void onKillClick() {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            consoleArea.appendText("\n--- PROCESS KILLED BY USER ---\n");
            killButton.setDisable(true);
        }
    }

    @FXML
    private void onCloseClick() {
        stage.close();
    }
}
