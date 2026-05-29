package dev.project516.sandbox.gui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LauncherHome extends Application {

    @Override
    public void start(Stage stage) {
        Label l = new Label("Welcome");

        Button button1 = new Button("Click me");

        VBox root = new VBox(10, l, button1);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.setTitle("Sandbox");
        stage.show();

        button1.setOnAction(e -> l.setText("Clicked!"));
    }
}
