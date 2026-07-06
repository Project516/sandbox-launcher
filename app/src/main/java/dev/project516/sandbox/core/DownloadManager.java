package dev.project516.sandbox.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project516.sandbox.model.mojang.*;
// import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/** Download manager **/
public class DownloadManager {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static void downloadFile(String fileUrl, Path destination) {
        downloadFile(fileUrl, destination, progress -> {});
    }
    /** General Download file method **/
    public static void downloadFile(String fileUrl, Path destination, java.util.function.Consumer<Double> onProgress) {
        try {
            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }

            HttpRequest request =
                    HttpRequest.newBuilder().uri(URI.create(fileUrl)).build();

            System.out.println("[DOWNLOAD] Fetching " + fileUrl + "...");

            HttpResponse<java.io.InputStream> response =
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {

                long totalBytes =
                        response.headers().firstValueAsLong("Content-Length").orElse(1L);
                long downloadedBytes = 0;

                try (java.io.InputStream inputStream = response.body();
                        java.io.OutputStream outputStream = Files.newOutputStream(destination)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        downloadedBytes = downloadedBytes + bytesRead;
                        double progress = (double) downloadedBytes / totalBytes;
                        onProgress.accept(progress);
                    }
                }
                System.out.println("[DOWNLOAD] Saved to " + destination);
            } else {
                System.err.println("[DOWNLOAD] Failed. HTTP Code: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("[DOWNLOAD] Error downloading file.");
            e.printStackTrace();
        }
    }

    /** Download Minecraft client Jar**/
    public static void downloadClientJar(
            Path versionJsonPath, String mcVersion, java.util.function.Consumer<Double> onProgress) {

        try {
            VersionInfo info = MAPPER.readValue(versionJsonPath.toFile(), VersionInfo.class);

            if (info.downloads() == null || info.downloads().client() == null) {
                System.err.println("[DOWNLOAD] JSON did not contain client download info.");
                return;
            }

            String clientUrl = info.downloads().client().url();
            System.out.println("[DOWNLOAD] Found client JAR URL: " + clientUrl);

            Path jarPath = versionJsonPath.getParent().resolve(mcVersion + ".jar");

            downloadFile(clientUrl, jarPath, onProgress);

        } catch (Exception e) {
            System.err.println("[DOWNLOAD] Failed to parse version JSON.");
            e.printStackTrace();
        }
    }

    /** Download required libraries for Minecraft **/
    public static void downloadLibraries(Path versionJsonPath, java.util.function.Consumer<Double> onProgress) {
        try {
            VersionInfo info = MAPPER.readValue(versionJsonPath.toFile(), VersionInfo.class);
            if (info.libraries() == null) return;

            Path libBaseDir = Path.of(System.getProperty("user.home"), ".sandbox-launcher", "libraries");

            int totalLibraries = info.libraries().size();
            int completedLibraries = 0;

            for (Library lib : info.libraries()) {
                if (lib.downloads() == null || lib.downloads().artifact() == null) continue;

                Artifact artifact = lib.downloads().artifact();
                Path libPath = libBaseDir.resolve(artifact.path());

                if (!Files.exists(libPath)) {
                    downloadFile(artifact.url(), libPath);
                }

                completedLibraries++;
                double progress = (double) completedLibraries / totalLibraries;
                onProgress.accept(progress);
            }

            System.out.println("[DOWNLOAD] All libraries fetched.");
        } catch (Exception e) {
            System.err.println("[DOWNLOAD] Failed to parse libraries.");
            e.printStackTrace();
        }
    }

    /** Downloads assets required for Minecraft**/
    public static void downloadAssets(VersionInfo info, java.util.function.Consumer<Double> onProgress) {
        if (info.assetIndex() == null) {
            System.err.println("[DOWNLOAD] No asset index found in version JSON.");
            return;
        }

        Path assetsDir = Path.of(System.getProperty("user.home"), ".sandbox-launcher", "assets");
        Path indexesDir = assetsDir.resolve("indexes");
        Path objectsDir = assetsDir.resolve("objects");

        try {
            Path indexPath = indexesDir.resolve(info.assetIndex().id() + ".json");
            downloadFile(info.assetIndex().url(), indexPath);

            AssetObjects assetObjects = MAPPER.readValue(indexPath.toFile(), AssetObjects.class);
            if (assetObjects.objects() == null) return;

            int totalAssets = assetObjects.objects().size();
            System.out.println("[DOWNLOAD] Found " + totalAssets + " assets to download.");

            int completedAssets = 0;

            for (Map.Entry<String, AssetObject> entry : assetObjects.objects().entrySet()) {
                AssetObject asset = entry.getValue();
                String hash = asset.hash();

                String subDir = hash.substring(0, 2);
                Path assetPath = objectsDir.resolve(subDir).resolve(hash);

                if (!Files.exists(assetPath)) {
                    String assetUrl = "https://resources.download.minecraft.net/" + subDir + "/" + hash;
                    downloadFile(assetUrl, assetPath);
                }

                completedAssets++;
                double progress = (double) completedAssets / totalAssets;
                onProgress.accept(progress);
            }
            System.out.println("[DOWNLOAD] All assets fetched.");

        } catch (Exception e) {
            System.err.println("[DOWNLOAD] Failed to process assets.");
            e.printStackTrace();
        }
    }

    /** Compatibility with older versions **/
    public static void extractNatives(Path versionJsonPath, String mcVersion) {
        try {
            VersionInfo info = MAPPER.readValue(versionJsonPath.toFile(), VersionInfo.class);
            if (info.libraries() == null) return;

            Path nativesDir = versionJsonPath.getParent().resolve("natives");
            Files.createDirectories(nativesDir);

            for (Library lib : info.libraries()) {
                if (lib.downloads() == null || lib.downloads().classifiers() == null) continue;

                Artifact nativeArtifact = lib.downloads().classifiers().get("natives-linux");
                if (nativeArtifact == null) continue;

                Path tempJar = versionJsonPath.getParent().resolve("natives-temp.jar");
                downloadFile(nativeArtifact.url(), tempJar);

                System.out.println("[DOWNLOAD] Extracted natives for " + lib.name());

                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(tempJar.toFile())) {
                    Path normalizedNativesDir = nativesDir.toAbsolutePath().normalize();
                    java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        java.util.jar.JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".so")) {
                            Path outFile = normalizedNativesDir
                                    .resolve(entry.getName())
                                    .normalize();
                            if (!outFile.startsWith(normalizedNativesDir)) {
                                System.err.println("[DOWNLOADS] Skipping suspicious native entry: " + entry.getName());
                                continue;
                            }
                            Files.createDirectories(outFile.getParent());
                            Files.copy(jar.getInputStream(entry), outFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }

                Files.deleteIfExists(tempJar);
            }
            System.out.println("[DOWNLOADS] Natives extracted.");
        } catch (Exception e) {
            System.err.println("[DOWNLOADS] Failed to extract natives.");
            e.printStackTrace();
        }
    }
}
