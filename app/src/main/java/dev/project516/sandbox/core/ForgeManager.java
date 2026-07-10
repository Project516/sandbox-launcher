package dev.project516.sandbox.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project516.sandbox.model.Instance;
import dev.project516.sandbox.model.ModdedProfile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(PROMOTIONS)).build();
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = MAPPER.readTree(resp.body());

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

        String versionId = mc + "-forge";
        Path versionDir = mcRoot.resolve("versions").resolve(versionId);
        Path versionJson = versionDir.resolve(versionId + ".json");
        if (!Files.exists(versionJson)) {
            throw new RuntimeException("Forge installer did not produce " + versionJson);
        }

        JsonNode vj = MAPPER.readTree(Files.readString(versionJson));
        String mainClass = vj.path("mainClass").asText("cpw.mods.modlauncher.Launcher");
        List<String> cp = new ArrayList<>();
        for (JsonNode lib : vj.path("libraries")) {
            JsonNode art = lib.path("downloads").path("artifact").path("path");
            if (art.isMissingNode()) cp.add("libraries/" + art.asText());
        }

        Path profilePath = mcRoot.resolve("versions").resolve(mc).resolve(mc + "-forge.json");
        Files.createDirectories(profilePath.getParent());
        ModdedProfile profile = new ModdedProfile("forge", mc, mainClass, cp);
        Files.writeString(profilePath, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(profile));

        if (progress != null) progress.accept(1.0);
        System.out.println("[FORGE] Installed " + forgeVer + " for " + mc);
    }
}
