package dev.project516.sandbox.screen;

// import dev.project516.sandbox.core.FabricManager;
import dev.project516.sandbox.model.Instance;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class InstanceSettingsController {

    @FXML
    private TextField nameField;

    private Instance instance;
    private Consumer<Instance> onSaveCallback;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
        nameField.setText(instance.name());
    }

    public void setOnSaveCallback(Consumer<Instance> onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    @FXML
    private void onSaveClick() {
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            newName = "Unnamed Instance";
        }

        Instance updatedInstance =
                new Instance(newName, instance.mcVersion(), instance.iconPath(), instance.modLoader());

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
