package dev.project516.sandbox.core;

import com.fasterxml.jackson.databind.JsonNode;
import dev.project516.sandbox.model.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Manage modloaders **/
public class ModLoaderManager {
    public static void install(Instance instance, Consumer<Double> progress) throws Exception {
        String loader = instance.modLoader() == null ? "vanilla" : instance.modLoader();
        switch (loader.toLowerCase()) {
            case "fabric" ->
                FabricManager.install(
                        instance,
                        progress); // we could add quilt later, but thats in beta still (i think). quilt is a fork of
            // fabric.
            case "forge" -> ForgeManager.install(instance, progress);
            case "neoforge" -> NeoForgeManager.install(instance, progress);
            case "vanilla" -> {}
            default -> throw new IllegalArgumentException("Unknown mod loader " + loader);
        }
    }

    static List<String> extractJvmArgs(JsonNode jvmArgs) {
        List<String> out = new ArrayList<>();
        if (!jvmArgs.isArray()) return out;

        List<JsonNode> tokens = new ArrayList<>();
        jvmArgs.forEach(tokens::add);

        for (int i = 0; i < tokens.size(); i++) {
            if (!tokens.get(i).isTextual()) continue;
            String arg = tokens.get(i).asText();
            if (arg.contains("${")) continue;

            if (arg.equals("--add-opens") || arg.equals("--add-exports") || arg.equals("--add-modules")) {
                out.add(arg);
                if (i + 1 < tokens.size() && tokens.get(i + 1).isTextual()) {
                    out.add(tokens.get(i + 1).asText());
                    i++;
                }
            } else if (arg.startsWith("-D") || arg.startsWith("-X")) {
                out.add(arg);
            }
        }
        return out;
    }

    static List<String> extractLoaderArgs(JsonNode gameArgs) {
        List<String> out = new ArrayList<>();
        if (!gameArgs.isArray()) return out;

        List<JsonNode> tokens = new ArrayList<>();
        gameArgs.forEach(tokens::add);

        for (int i = 0; i < tokens.size(); i++) {
            if (!tokens.get(i).isTextual()) continue;
            String arg = tokens.get(i).asText();
            if (!arg.startsWith("--launchTarget") && !arg.startsWith("--fml.")) continue;

            out.add(arg);
            if (i + 1 < tokens.size() && tokens.get(i + 1).isTextual()) {
                String value = tokens.get(i + 1).asText();
                if (!value.startsWith("--")) {
                    out.add(value);
                    i++;
                }
            }
        }
        return out;
    }
}
