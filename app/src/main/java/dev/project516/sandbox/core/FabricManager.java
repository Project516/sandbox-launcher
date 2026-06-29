package dev.project516.sandbox.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project516.sandbox.model.fabric.FabricLibrary;
import dev.project516.sandbox.model.fabric.FabricVersionInfo;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

public class FabricManager {
    private static final String FABRIC_API = "https://meta.fabricmc.net/v2/versions/loader/";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static FabricVersionInfo fetchLatestLoader(String mcVersion) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FABRIC_API + mcVersion))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            FabricVersionInfo[] versions = MAPPER.readValue(response.body(), FabricVersionInfo[].class);
            if (versions.length > 0) {
                return versions[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void downloadFabricLibraries(FabricVersionInfo info) {
        if (info == null || info.launcherMeta() == null || info.launcherMeta().libraries() == null) return;

        Path libBaseDir = Path.of(System.getProperty("user.home"), ".sandbox-launcher", "libraries");

        for (FabricLibrary lib : info.launcherMeta().libraries()) {
            String[] parts = lib.name().split(":");
            String groupPath = parts[0].replace('.', '/');
            String artifactId = parts[1];
            String version = parts[2];

            String mavenPath = groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
            Path localPath = libBaseDir.resolve(mavenPath);

            String fullUrl = lib.url() + mavenPath;
            DownloadManager.downloadFile(fullUrl, localPath);
        }

        System.out.println("[FABRIC] All Fabric libraries fetched.");
    }
}
