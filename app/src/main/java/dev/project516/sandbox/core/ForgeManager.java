package dev.project516.sandbox.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project516.sandbox.model.Instance;
import dev.project516.sandbox.model.ModdedProfile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Forge modloader **/
public class ForgeManager {
    private static final String PROMOTIONS =
            "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
    private static final String MAVEN = "https://maven.minecraftforge.net/net/minecraftforge/forge/";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /** Get latest version of Forge for selected Minecraft version **/
    public static String latestVersion(String mcVersion) throws Exception {
        Path cacheFile =
                Path.of(System.getProperty("user.home"), ".sandbox-launcher", "cache", "forge_promotions.json");
        String body = DownloadManager.fetchTextWithCache(PROMOTIONS, cacheFile, true);
        if (body == null) throw new RuntimeException("Failed to fetch Forge metadata");

        JsonNode root = MAPPER.readTree(body);
        JsonNode node = root.path("promos").path(mcVersion + "-latest");
        if (node.isMissingNode()) node = root.path("promos").path(mcVersion + "-recommended");
        if (node.isMissingNode()) throw new RuntimeException("No Forge build for " + mcVersion);
        return node.asText();
    }

    /** Install Forge modloader to selected Minecraft version **/
    public static void install(Instance instance, Consumer<Double> progress) throws Exception {
        String mc = instance.mcVersion();
        String forgeVer = latestVersion(mc);

        String installerName = "forge-" + mc + "-" + forgeVer + "-installer.jar";
        String installerUrl = MAVEN + mc + "-" + forgeVer + "/" + installerName;

        Path libBase = Path.of(System.getProperty("user.home"), ".sandbox-launcher", "libraries");
        Path installerDir = libBase.resolve("forge").resolve(mc);
        Files.createDirectories(installerDir);
        Path installerJar = installerDir.resolve(installerName);

        if (!Files.exists(installerJar)) {
            DownloadManager.downloadFile(installerUrl, installerJar, p -> {
                if (progress != null) progress.accept(p * 0.5);
            });
        }

        Path mcRoot = Path.of(System.getProperty("user.home"), ".sandbox-launcher");
        Path profilesJson = mcRoot.resolve("launcher_profiles.json");
        if (!Files.exists(profilesJson)) {
            Files.writeString(profilesJson, "{\"profiles\":{}, \"selectedProfile\": \"(Default)\"}");
        }
        List<String> cmd = List.of(
                "java",
                "-jar",
                installerJar.toAbsolutePath().toString(),
                "--installClient",
                mcRoot.toAbsolutePath().toString());

        System.out.println("[FORGE] Running installer: " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        Process proc = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) System.out.println("[FORGE]" + line);
        }

        int code = proc.waitFor();
        if (code != 0) throw new RuntimeException("Forge installer exited with " + code);

        Path versionsDir = mcRoot.resolve("versions");
        Path foundVersionJson = null;
        if (Files.exists(versionsDir)) {
            try (var stream = Files.walk(versionsDir)) {
                foundVersionJson = stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .filter(path -> path.getFileName().toString().contains(forgeVer))
                        .findFirst()
                        .orElse(null);
            }
        }
        if (foundVersionJson == null) {
            throw new RuntimeException("Forge installer did not produce a version JSON for " + forgeVer);
        }

        JsonNode vj = MAPPER.readTree(Files.readString(foundVersionJson));
        String mainClass = vj.path("mainClass").asText("cpw.mods.modlauncher.Launcher");
        List<String> cp = new ArrayList<>();
        for (JsonNode lib : vj.path("libraries")) {
            JsonNode art = lib.path("downloads").path("artifact").path("path");
            if (!art.isMissingNode()) {
                cp.add("libraries/" + art.asText());
            } else {
                String name = lib.path("name").asText("");
                if (!name.isEmpty()) {
                    String[] parts = name.split(":");
                    if (parts.length >= 3) {
                        String groupPath = parts[0].replace('.', '/');
                        String artifact = parts[1];
                        String version = parts[2];
                        String path = groupPath + "/" + artifact + "/" + version + "/" + artifact + "-" + version;
                        if (parts.length == 4) {
                            path += "-" + parts[3];
                        }
                        path += ".jar";
                        cp.add("libraries/" + path);
                    }
                }
            }
        }

        List<String> extraArgs = new ArrayList<>();
        JsonNode args = vj.path("arguments").path("game");
        if (args.isArray()) {
            for (JsonNode arg : args) {
                if (arg.isTextual()) {
                    String argStr = arg.asText();
                    if (argStr.startsWith("--launchTarget") || argStr.startsWith("--fml.")) {
                        extraArgs.add(argStr);
                    }
                }
            }
        }

        Path profilePath = mcRoot.resolve("versions").resolve(mc).resolve(mc + "-forge.json");
        Files.createDirectories(profilePath.getParent());
        ModdedProfile profile = new ModdedProfile("forge", mc, mainClass, cp, extraArgs);
        Files.writeString(profilePath, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(profile));

        if (progress != null) progress.accept(1.0);
        System.out.println("[FORGE] Installed " + forgeVer + " for " + mc);
    }
}
