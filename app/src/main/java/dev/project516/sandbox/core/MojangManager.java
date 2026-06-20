package dev.project516.sandbox.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project516.sandbox.model.mojang.VersionManifest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Fetches and parses version JSON from Mojang **/
public class MojangManager {
    private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static CompletableFuture<VersionManifest> fetchVersionManifest() {
        HttpRequest request =
                HttpRequest.newBuilder().uri(URI.create(MANIFEST_URL)).build();

        return HTTP_CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(json -> {
                    try {
                        return MAPPER.readValue(json, VersionManifest.class);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
    }
}
