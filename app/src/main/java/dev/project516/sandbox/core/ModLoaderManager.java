package dev.project516.sandbox.core;

import dev.project516.sandbox.model.Instance;
import java.util.function.Consumer;

/** Manage modloaders **/
public class ModLoaderManager {
    public static void install(Instance instance, Consumer<Double> progress) throws Exception {
        String loader = instance.modLoader() == null ? "vanilla" : instance.modLoader();
        switch (loader.toLowerCase()) {
            case "fabric" -> FabricManager.install(instance, progress); // we could add quilt later, but thats in beta still (i think). quilt is a fork of fabric.
            case "forge" -> ForgeManager.install(instance, progress);
            case "neoforge" -> NeoForgeManager.install(instance, progress);
            case "vanilla" -> {}
            default -> throw new IllegalArgumentException("Unknown mod loader " + loader);
        }
    }
}
