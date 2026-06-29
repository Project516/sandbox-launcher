package dev.project516.sandbox.screen;

import dev.project516.sandbox.model.Instance;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class InstanceListCell extends ListCell<Instance> {
    private final ImageView iconView = new ImageView();
    private final Label nameLabel = new Label();
    private final Label versionLabel = new Label();
    private final VBox textBox = new VBox(nameLabel, versionLabel);
    private final HBox layout = new HBox(10, iconView, textBox);

    public InstanceListCell() {
        super();

        setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);

        iconView.setFitWidth(32);
        iconView.setFitHeight(32);
        iconView.setPreserveRatio(true);

        try {
            Image icon = new Image(getClass().getResource("icon.png").toExternalForm());
            iconView.setImage(icon);
        } catch (Exception e) {
            System.err.println("Could not load icon.png: " + e.getMessage());
        }

        versionLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");

        setPrefHeight(40);
    }

    @Override
    protected void updateItem(Instance instance, boolean empty) {
        super.updateItem(instance, empty);

        if (empty || instance == null) {
            setGraphic(null);
        } else {
            nameLabel.setText(instance.name());
            versionLabel.setText("Minecraft " + instance.mcVersion());

            if (instance.iconPath() != null && Files.exists(Path.of(instance.iconPath()))) {
                iconView.setImage(new Image("file:" + instance.iconPath()));
            } else {
                try {
                    iconView.setImage(
                            new Image(getClass().getResource("icon.png").toExternalForm()));
                } catch (Exception e) {
                    System.err.println("Could not load default icon.png.");
                }
            }

            setGraphic(layout);
        }
    }
}
