package dev.project516.sandbox.core;

import dev.project516.sandbox.model.Instance;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Manages launching Minecraft in a sandbox **/
public class SandboxManager {

    public static Process linuxLaunchInstanceInDocker(Instance instance, Consumer<String> logConsumer) {

        String osName = System.getProperty("os.name").toLowerCase();

        try {
            String mcVersion = instance.mcVersion();
            String home = System.getProperty("user.home");
            String instanceDir = home + "/.sandbox-launcher";
            String jarPath = "/app/versions/" + mcVersion + "/" + mcVersion + ".jar";

            Path localLibDir = Path.of(home, ".sandbox-launcher", "libraries");
            List<String> jarPaths = new ArrayList<>();

            Path versionJsonPath = Path.of(home, ".sandbox-launcher", "versions", mcVersion, mcVersion + ".json");

            if (Files.exists(versionJsonPath)) {
                dev.project516.sandbox.model.mojang.VersionInfo info = DownloadManager.MAPPER.readValue(
                        versionJsonPath.toFile(), dev.project516.sandbox.model.mojang.VersionInfo.class);

                if (info.libraries() != null) {
                    for (dev.project516.sandbox.model.mojang.Library lib : info.libraries()) {
                        if (lib.downloads() == null || lib.downloads().artifact() == null) continue;

                        String libPath = lib.downloads().artifact().path();
                        jarPaths.add("/app/libraries/" + libPath);
                    }
                }
            } else {
                logConsumer.accept("[DOCKER] WARNING: Could not find version JSON at " + versionJsonPath);
            }

            String classpath = jarPath + ":" + String.join(":", jarPaths);
            String javaImage = getJavaImageForVersion(mcVersion);

            Path localJarPath = Path.of(home, ".sandbox-launcher", "versions", mcVersion, mcVersion + ".jar");
            logConsumer.accept("[DEBUG Local JAR exists: " + Files.exists(localJarPath) + " (" + localJarPath + ")");
            logConsumer.accept("[DEBUG] Classpath: " + classpath);

            logConsumer.accept("[DOCKER] Launching with image: " + javaImage);

            List<String> command = new ArrayList<>(List.of("docker", "run", "--rm"));

            if (osName.contains("linux")) {
                command.addAll(List.of(
                        "-e",
                        "DISPLAY=:0",
                        "-e",
                        "XDG_RUNTIME_DIR=/run/user/1000",
                        "-e",
                        "PULSE_SERVER=unix:/run/user/1000/pulse/native",
                        "-e",
                        "LIBGL_ALWAYS_SOFTWARE=1",
                        "-v",
                        "/tmp/.X11-unix:/tmp/.X11-unix",
                        "-v",
                        "/run/user/1000:/run/user/1000",
                        "--device",
                        "/dev/dri",
                        "--device",
                        "/dev/snd",
                        "-v",
                        "/dev/input:/dev/input"));
            }

            command.addAll(List.of("-v", instanceDir + ":/app", javaImage, "java"));

            if (!javaImage.equals("sandbox-java8")) {
                command.add("--enable-native-access=ALL-UNNAMED");
            }

            boolean isOldVersion = false;
            try {
                String[] parts = mcVersion.split("\\.");
                if (parts.length >= 2) {
                    int major = Integer.parseInt(parts[0]);
                    int minor = Integer.parseInt(parts[1]);
                    if (major == 1 && minor <= 5) {
                        isOldVersion = true;
                    }
                }
            } catch (Exception ignored) {
            }

            // if (instance.modLoader().equalsIgnoreCase("fabric")) {
            //     logConsumer.accept("[DEBUG] Launching with Fabric Mod Loader!");
            //     command.addAll(List.of(
            //             "-Djava.library.path=/app/versions/" + mcVersion + "/natives",
            //             "-cp",
            //             classpath,
            //             "net.fabricmc.loader.launch.knot.Client",
            //             "--username",
            //             PlayerManager.getUsername(),
            //             "--version",
            //             mcVersion,
            //             "--gameDir",
            //             "/app/instances/" + mcVersion,
            //             "--assetsDir",
            //             "/app/assets",
            //             "--accessToken",
            //             "0"));
            // } else
            if (isOldVersion) {
                command.addAll(List.of(
                        "-Djava.library.path=/app/versions/" + mcVersion + "/natives",
                        "-Dhttp.proxyHost=betacraft.uk", // betacraft proxy for 1.0.0-rc1 to 1.5.2 :
                        // https://betacraft.uk/blog/post/how-to-use-betacraft-proxy
                        "-Dhttp.proxyPort=11702",
                        "-Dminecraft.applet.TargetDirectory=/app/instances/" + mcVersion,
                        "-cp",
                        classpath,
                        "net.minecraft.client.Minecraft",
                        PlayerManager.getUsername(),
                        "-token",
                        "0"));
            } else {
                command.addAll(List.of(
                        "-Djava.library.path=/app/versions/" + mcVersion + "/natives",
                        "-cp",
                        classpath,
                        "net.minecraft.client.main.Main",
                        "--version",
                        mcVersion,
                        "--accessToken",
                        "0",
                        "--gameDir",
                        "/app/instances/" + mcVersion,
                        "--assetsDir",
                        "/app/assets",
                        "--username",
                        PlayerManager.getUsername()));
            }

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            new Thread(() -> {
                        try (BufferedReader reader =
                                new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                logConsumer.accept("[DOCKER] " + line);
                            }
                            int exitCode = process.waitFor();
                            logConsumer.accept("[DOCKER] Instance exited with code: " + exitCode);
                        } catch (Exception e) {
                            logConsumer.accept("[DOCKER] Error reading process output: " + e.getMessage());
                        }
                    })
                    .start();

            return process;

        } catch (Exception e) {
            logConsumer.accept("[DOCKER] Failed to launch instance: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /** Determine Java version for Minecraft version **/
    private static String getJavaImageForVersion(String mcVersion) {
        try {
            String[] parts = mcVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int patch = (parts.length >= 3) ? Integer.parseInt(parts[2]) : 0;

                if (major >= 26) {
                    System.out.println("[DOCKER] Using Java 25");
                    return "sandbox-java25";
                }

                if (major == 1) {
                    if (minor <= 16) {
                        System.out.println("[DOCKER] Using Java 8.");
                        return "sandbox-java8";
                    } else if (minor == 17) {
                        System.out.println("[DOCKER] Using Java 16.");
                        return "sandbox-java16";
                    } else if (minor >= 18 && minor <= 21) {
                        if ((minor == 20 && patch >= 5) || minor == 21) {
                            System.out.println("[DOCKER] Using Java 21.");
                            return "sandbox-java21";
                        }
                        System.out.println("[DOCKER] Using Java 17.");
                        return "sandbox-java17";
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not parse version " + mcVersion + " defaulting to Java 25.");
        }
        System.out.println("[DOCKER] Using Java 25.");
        return "sandbox-java25";
    }
}
