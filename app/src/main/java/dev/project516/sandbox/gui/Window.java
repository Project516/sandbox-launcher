package dev.project516.sandbox.gui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Window extends Application {

    @Override
    public void start(Stage stage) {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        Label l =
                new Label(
                        "Hello, JavaFX "
                                + javafxVersion
                                + ", running on Java "
                                + javaVersion
                                + ".");

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
