package dev.project516.sandbox.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project516.sandbox.model.mojang.VersionManifest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Fetches and parses version JSON from Mojang **/
public class MojangManager {
    private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final Path MANIFEST_FILE =
            Path.of(System.getProperty("user.home"), ".sandbox-launcher", "manifest.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /** see if version manifest exists locally **/
    public static VersionManifest loadLocalManifest() { // we save it locally to speed up selecting an instance so we don't have to redownload each time
        try {
            if (Files.exists(MANIFEST_FILE)) {
                return MAPPER.readValue(MANIFEST_FILE.toFile(), VersionManifest.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Download version manifest **/
    public static CompletableFuture<VersionManifest> fetchVersionManifest() {
        HttpRequest request =
                HttpRequest.newBuilder().uri(URI.create(MANIFEST_URL)).build();

        return HTTP_CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(json -> {
                    try {
                        VersionManifest manifest = MAPPER.readValue(json, VersionManifest.class);
                        Files.createDirectories(MANIFEST_FILE.getParent());
                        Files.writeString(MANIFEST_FILE, json);
                        return manifest;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
    }
}
