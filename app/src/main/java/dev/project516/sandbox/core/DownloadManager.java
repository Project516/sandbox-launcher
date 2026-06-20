package dev.project516.sandbox.core;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloadManager {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void downloadFile(String fileUrl, Path destination) {
        try {
            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }

            HttpRequest request =
                    HttpRequest.newBuilder().uri(URI.create(fileUrl)).build();

            System.out.println("[DOWNLOAD] Fetching" + fileUrl + "...");

            HttpResponse<Path> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(destination));

            if (response.statusCode() == 200) {
                System.out.println("[DOWNLOAD] Saved to " + destination);
            } else {
                System.err.println("[DOWNLOAD] Failed. HTTP Code: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("[DOWNLOAD] Error downloading file.");
            e.printStackTrace();
        }
    }
}
