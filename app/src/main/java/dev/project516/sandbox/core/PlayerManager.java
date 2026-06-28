package dev.project516.sandbox.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project516.sandbox.model.mojang.Settings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Manage Player settings **/
public class PlayerManager {
    private static final Path CONFIG_DIR = Path.of(System.getProperty("user.home"), ".sandbox-launcher");
    private static final Path SETTINGS_FILE = CONFIG_DIR.resolve("settings.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Settings settings = loadSettings();

    private static Settings loadSettings() {
        try {
            if (!Files.exists(SETTINGS_FILE)) {
                return new Settings();
            }
            return MAPPER.readValue(Files.readString(SETTINGS_FILE), Settings.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new Settings();
        }
    }

    public static void saveSettings(String username) {
        settings = new Settings(username);
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(settings);
            Files.writeString(SETTINGS_FILE, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getUsername() {
        return settings.username();
    }
}
