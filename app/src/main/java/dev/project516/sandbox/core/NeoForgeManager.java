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

public class NeoForgeManager {
    private static final String MAVEN_METADATA =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
    private static final String MAVEN = "https://maven.neoforged.net/releases/net/neoforged/neoforge/";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static String latestVersion(String mcVersion) throws Exception {
        HttpRequest req =
                HttpRequest.newBuilder().uri(URI.create(MAVEN_METADATA)).build();
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

        String body = resp.body();
        String latest = extractTag(body, "latest");
        if (latest == null) {
            throw new RuntimeException("Could not parse NeoForge maven-metadata.xml");
        }
        return latest;
    }

    private static String extractTag(String xml, String tag) {
        int s = xml.indexOf("<" + tag + ">");
        int e = xml.indexOf("</" + tag + ">");
        if (s < 0 && e < 0) return null;
        return xml.substring(s + tag.length() + 2, e).trim();
    }

    public static void install(Instance instance, Consumer<Double> progress) throws Exception {
        String mc = instance.mcVersion();
        String nfVer = latestVersion(mc);

        String installerName = "neoforge-" + nfVer + "-installer.jar";
        String installerUrl = MAVEN + nfVer + "/" + installerName;

        Path libBase = Path.of(System.getProperty("user.home"), ".sandbox-launcher", "libraries");
        Path installerDir = libBase.resolve("neoforge").resolve(mc);
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

        System.out.println("[NEOFORGE] Running installer " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        Process proc = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) System.out.println("[NEOFORGE]" + line);
        }
        int code = proc.waitFor();
        if (code != 0) throw new RuntimeException("NeoForge installer exited with " + code);

        String versionId = "neoforge-" + mc + "-" + nfVer;
        Path versionDir = mcRoot.resolve("versions").resolve(versionId);
        Path versionJson = versionDir.resolve(versionId + ".json");
        if (!Files.exists(versionJson)) {
            throw new RuntimeException("Neoforge installer did not produce " + versionJson);
        }

        JsonNode vj = MAPPER.readTree(Files.readString(versionJson));
        String mainClass = vj.path("mainClass").asText("cpw.mods.modlauncher.Launcher");
        List<String> cp = new ArrayList<>();
        for (JsonNode lib : vj.path("libraries")) {
            JsonNode art = lib.path("downloads").path("artifact").path("path");
            if (!art.isMissingNode()) cp.add("libraries/" + art.asText());
        }

        Path profilePath = mcRoot.resolve("versions").resolve(mc).resolve(mc + "-neoforge.json");
        Files.createDirectories(profilePath.getParent());
        ModdedProfile profile = new ModdedProfile("neoforge", mc, mainClass, cp);
        Files.writeString(profilePath, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(profile));

        if (progress != null) progress.accept(1.0);
        System.out.println("[NEOFORGE] Installed " + nfVer + " for " + mc);
    }
}
