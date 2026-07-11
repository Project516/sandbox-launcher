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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** NeoForge modloader manager **/
public class NeoForgeManager { // NeoForge is a fork of Forge that's modern
    private static final String MAVEN_METADATA =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
    private static final String MAVEN = "https://maven.neoforged.net/releases/net/neoforged/neoforge/";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static String latestVersion(String mcVersion) throws Exception {
        Path cacheFile =
                Path.of(System.getProperty("user.home"), ".sandbox-launcher", "cache", "neoforge_metadata.xml");
        String body = DownloadManager.fetchTextWithCache(MAVEN_METADATA, cacheFile);
        if (body == null) throw new RuntimeException("Failed to fetch NeoForge metadata");

        List<String> versions = new ArrayList<>();
        Pattern p = Pattern.compile("<version>([^<]+)</version>");
        Matcher m = p.matcher(body);
        while (m.find()) {
            versions.add(m.group(1));
        }

        String mcPrefix = mcVersion;
        if (mcVersion.startsWith("1.")) {
            String[] parts = mcVersion.split("\\.");
            if (parts.length >= 3) {
                mcPrefix = parts[1] + "." + parts[2]; // 1.20.4 -> 20.4
            } else if (parts.length == 2) {
                mcPrefix = parts[1]; // 1.20 -> 20
            }
        }

        String latest = null;
        for (String v : versions) {
            if (v.startsWith(mcPrefix)) {
                if (latest == null || v.compareTo(latest) > 0) {
                    latest = v;
                }
            }
        }

        if (latest == null) {
            throw new RuntimeException("Could not parse NeoForge maven-metadata.xml");
        }
        return latest;
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

        Path versionDir = mcRoot.resolve("versions");
        Path foundVersionJson = null;
        if (Files.exists(versionDir)) {
            try (var stream = Files.walk(versionDir)) {
                foundVersionJson = stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .filter(path -> path.getFileName().toString().contains(nfVer))
                        .findFirst()
                        .orElse(null);
            }
        }
        if (foundVersionJson == null) {
            throw new RuntimeException("NeoForge installer did not produce a version JSON for " + nfVer);
        }

        JsonNode vj = MAPPER.readTree(Files.readString(foundVersionJson));
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
