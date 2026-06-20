package dev.project516.sandbox.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project516.sandbox.model.Instance;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Instance Manager - Manages File IO **/
public class InstanceManager {
    private static final Path CONFIG_DIR = Path.of(System.getProperty("user.home"), ".sandbox-launcher");
    private static final Path INSTANCES_FILE = CONFIG_DIR.resolve("instances.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** loads instance **/
    public static List<Instance> loadInstances() {
        try {
            if (!Files.exists(INSTANCES_FILE)) {
                return new ArrayList<>();
            }

            return MAPPER.readValue(Files.readString(INSTANCES_FILE), new TypeReference<List<Instance>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /** saves instance to disk **/
    public static void saveInstances(List<Instance> instances) {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(instances);

            Files.writeString(INSTANCES_FILE, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
