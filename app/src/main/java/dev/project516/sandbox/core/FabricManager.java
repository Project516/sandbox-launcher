package dev.project516.sandbox.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project516.sandbox.model.Instance;
import dev.project516.sandbox.model.ModdedProfile;
import dev.project516.sandbox.model.fabric.FabricLibrary;
import dev.project516.sandbox.model.fabric.FabricVersionInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Managing the Fabric modloader **/
public class FabricManager {
    private static final String FABRIC_META = "https://meta.fabricmc.net/v2/versions/loader/";
    private static final String MAVEN_BASE = "https://maven.fabricmc.net/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** fetch latest fabric version for Minecraft version **/
    public static FabricVersionInfo fetchLatestLoader(String mcVersion) {
        Path cacheFile = Path.of(
                System.getProperty("user.home"), ".sandbox-launcher", "cache", "fabric_loader_" + mcVersion + ".json");
        String body = DownloadManager.fetchTextWithCache(
                FABRIC_META + mcVersion, cacheFile, true); // background refresh is part of local cache

        if (body == null) {
            System.err.println("[FABRIC] Failed to fetch loader metadata for " + mcVersion);
            return null;
        }

        try {
            FabricVersionInfo[] versions = MAPPER.readValue(body, FabricVersionInfo[].class);
            if (versions.length == 0) {
                System.out.println("[FABRIC] API returned empty array for " + mcVersion);
            }
            return versions.length > 0 ? versions[0] : null;
        } catch (Exception e) {
            System.err.println("[FABRIC] Failed to parse cached JSON. Deleting cache and retrying...");
            e.printStackTrace();

            try {
                Files.deleteIfExists(cacheFile);
                String freshBody = DownloadManager.fetchTextWithCache(FABRIC_META + mcVersion, cacheFile, false);
                if (freshBody == null) return null;

                FabricVersionInfo[] versions = MAPPER.readValue(freshBody, FabricVersionInfo[].class);
                return versions.length > 0 ? versions[0] : null;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    /** Install fabric **/
    public static void install(Instance instance, Consumer<Double> progress) {
        String mc = instance.mcVersion();
        FabricVersionInfo info = fetchLatestLoader(mc);
        if (info == null || info.launcherMeta() == null) {
            throw new RuntimeException("No Fabric loader available for " + mc);
        }

        Path libBase = Path.of(System.getProperty("user.home"), ".sandbox-launcher", "libraries");
        List<String> extraClasspath = new ArrayList<>();

        List<FabricLibrary> libs = new ArrayList<>();
        if (info.launcherMeta().libraries() != null) {
            if (info.launcherMeta().libraries().client() != null) {
                libs.addAll(info.launcherMeta().libraries().client());
            }
            if (info.launcherMeta().libraries().common() != null) {
                libs.addAll(info.launcherMeta().libraries().common());
            }
        }

        libs.add(new FabricLibrary("net.fabricmc:fabric-loader:" + info.loader().version(), MAVEN_BASE, null, 0));
        libs.add(new FabricLibrary(
                "net.fabricmc:intermediary:" + info.intermediary().version(), MAVEN_BASE, null, 0));

        int total = libs.size();
        int done = 0;
        for (FabricLibrary lib : libs) {
            String mavenPath = mavenPathOf(lib.name());
            Path local = libBase.resolve(mavenPath);
            if (!Files.exists(local)) {
                DownloadManager.downloadFile(lib.url() + mavenPath, local);
            }
            extraClasspath.add("libraries/" + mavenPath);

            done++;
            if (progress != null) progress.accept((double) done / total);
        }

        String mainClass = "net.fabricmc.loader.launch.knot.Client";
        if (info.launcherMeta().mainClass() != null) {
            JsonNode mcNode = info.launcherMeta().mainClass();
            if (mcNode.isTextual()) {
                mainClass = mcNode.asText();
            } else if (mcNode.isObject() && mcNode.has("client")) {
                mainClass = mcNode.get("client").asText();
            }
        }
        writeProfile(instance, mainClass, extraClasspath);
        System.out.println("[FABRIC] Installed loader " + info.loader().version() + " for " + mc);
    }

    /** navigating fabric maven **/
    static String mavenPathOf(String coords) {
        String[] parts = coords.split(":");
        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
    }

    static void writeProfile(Instance instance, String mainClass, List<String> cp) {
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".sandbox-launcher", "versions", instance.mcVersion());
            Files.createDirectories(dir);
            Path out = dir.resolve(instance.mcVersion() + "-fabric.json");
            ModdedProfile profile =
                    new ModdedProfile("fabric", instance.mcVersion(), mainClass, cp, List.of(), List.of());
            Files.writeString(
                    out, DownloadManager.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(profile));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
