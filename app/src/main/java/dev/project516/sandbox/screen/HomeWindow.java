package dev.project516.sandbox.screen;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HomeWindow extends Application {
    public void start(Stage primaryStage) throws Exception {

        Button launchButton = new Button("Launch");

        VBox homeLayout = new VBox(15);

        homeLayout.getChildren().add(launchButton);

        launchButton.setOnAction(e -> {
            launchButton.setDisable(true);

            new Thread(() -> {
                        System.out.println("Launching...");
                        //Application.launch(InstanceWindow.class); // need to find how to launch another window
                    })
                    .start();
        });

        Scene scene = new Scene(homeLayout, 600, 400);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Sandbox Launcher");
        primaryStage.show();
    }
}
