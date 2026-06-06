package dev.project516.sandbox.screen;

import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class InstanceWindow extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        TextField logField = new TextField();
        logField.setPromptText("Log Messages");
        logField.setDisable(true);

        Button launchButton = new Button("Launch");

        VBox launchLayout = new VBox(10);

        launchLayout.getChildren().addAll(logField, launchButton);
    }
}
