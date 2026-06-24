package dev.project516.sandbox.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Manages launching Minecraft in a sandbox **/
public class SandboxManager {

    public static void launchInstanceInDocker(String mcVersion) {
        try {
            String home = System.getProperty("user.home");
            String instanceDir = home + "/.sandbox-launcher";
            String jarPath = "/app/versions/" + mcVersion + "/" + mcVersion + ".jar";

            Path localLibDir = Path.of(home, ".sandbox-launcher", "libraries");
            List<String> jarPaths = new ArrayList<>();

            if (Files.exists(localLibDir)) {
                Files.walk(localLibDir)
                        .filter(p -> p.toString().endsWith(".jar"))
                        .forEach(p -> {
                            String dockerPath = p.toString().replace(instanceDir, "/app");
                            jarPaths.add(dockerPath);
                        });
            }

            String classpath = jarPath + ":" + String.join(":", jarPaths);
            String javaImage = getJavaImageForVersion(mcVersion);

            System.out.println("[DOCKER] Launching with image: " + javaImage);

            List<String> command = new ArrayList<>(List.of(
                    "docker",
                    "run",
                    "--rm",
                    "-e",
                    "DISPLAY=:0",
                    "-v",
                    "/tmp/.X11-unix:/tmp/.X11-unix",
                    "--device",
                    "/dev/dri",
                    "-v",
                    instanceDir + ":/app",
                    javaImage,
                    "java"));

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

            if (isOldVersion) {
                command.addAll(List.of(
                        "-Djava.library.path=/app/versions/" + mcVersion + "/natives", // ADD THIS
                        "-cp",
                        classpath,
                        "net.minecraft.client.Minecraft",
                        "-token",
                        "0",
                        "--gameDir",
                        "/app/instances/" + mcVersion,
                        "--assetsDir",
                        "/app/assets"));
            } else {
                command.addAll(List.of(
                        "-Djava.library.path=/app/versions/" + mcVersion + "/natives", // ADD THIS
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
                        "SandboxPlayer"));
            }

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[DOCKER] " + line);
            }

            int exitCode = process.waitFor();
            System.out.println("[DOCKER] Instance exited with code: " + exitCode);

        } catch (Exception e) {
            System.err.println("Failed to launch instance in Docker.");
            e.printStackTrace();
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
                    System.out.println("[DOCKER] Using Java 25 for version 26.x+");
                    return "sandbox-java25";
                }

                if (major == 1) {
                    if (minor <= 16) {
                        System.out.println("[DOCKER] Using custom Java 8 image.");
                        return "sandbox-java8";
                    } else if (minor == 17) {
                        System.out.println("[DOCKER] Using Java 16 for older versions.");
                        return "sandbox-java16";
                    } else if (minor >= 18 && minor <= 21) {
                        if ((minor == 20 && patch >= 5) || minor == 21) {
                            System.out.println("[DOCKER] Using custom Java 21 image.");
                            return "sandbox-java21";
                        }
                        System.out.println("[DOCKER] Using Java 17 for versions 1.18 to 1.20.4");
                        return "sandbox-java17";
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not parse version " + mcVersion + " defaulting to Java 21.");
        }
        System.out.println("[DOCKER] Using default Java 21.");
        return "sandbox-java21";
    }
}
