package dev.project516.sandbox.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SandboxManager {

    public static void launchInstanceInDocker(String mcVersion) {
        try {
            String home = System.getProperty("user.home");
            String instanceDir = home + "/.sandbox-launcher";
            String jarPath = "/app/versions/" + mcVersion + "/" + mcVersion + ".jar";

            Path localLibDir = Path.of(home + "/.sandbox-launcher", "libraries");
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

            System.out.println("[DOCKER] Built classpath with: " + jarPaths.size() + " libraries.");

            String nativeAccessFlag = "";
            if (!javaImage.equals("eclipse-temurin:8-jdk")) {
                nativeAccessFlag = "--enable-native-access=ALL-UNNAMED ";
            }

            String shellCommand = "apt-get update && " +
                    "apt-get install -y libgl1 libegl1 libglfw3 libgl1-mesa-dri && " +
                    "java " + nativeAccessFlag + "-cp \"" + classpath + "\" net.minecraft.client.main.Main " +
                    "--version " + mcVersion + " --accessToken 0 --gameDir /app/instances/" + mcVersion +
                    " --assetsDir /app/assets --username SandboxPlayer";
            ProcessBuilder builder = new ProcessBuilder(
                    "docker",
                    "run",
                    "--rm",
                    "--network",
                    "host",
                    "-e",
                    "DISPLAY=:0",
                    "-v",
                    "/tmp/.X11-unix:/tmp/.X11-unix",
                    "--device",
                    "/dev/dri",
                    "-v",
                    instanceDir + ":/app",
                    javaImage,
                    "sh",
                    "-c",
                    shellCommand);

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

    // Version List (needs verification)
    //
    // 1.16 and below  - java 8
    // 1.17            - java 16 (?)
    // 1.18 - 1.20.4   - java 17
    // 1.20.5 - 1.21.6 - java 21
    // 26.x - ...      - java 25
    private static String getJavaImageForVersion(String mcVersion) {
        try {
            String[] parts = mcVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);

                int patch = (parts.length >= 3) ? Integer.parseInt(parts[2]) : 0;

                if (major >= 26) {
                    System.out.println("[DOCKER] Using Java 25 for version 26.x+");
                    return "eclipse-temurin:25-jdk";
                }

                if (major == 1) {

                    if (minor <= 16) {
                        System.out.println("[DOCKER] Using Java 8 for older versions.");
                        return "eclipse-temurin:8-jdk";
                    } else if (minor == 17) {
                        System.out.println("[DOCKER] Using Java 16 for older versions.");
                        return "eclipse-temurin:16-jdk";
                    } else if (minor >= 18 && minor <= 21) {
                        if ((minor == 20 && patch >= 5) || minor == 21) {
                            System.out.println("[DOCKER] Using Java 21 for modern versions.");
                            return "eclipse-temurin:21-jdk";
                        }
                        System.out.println("[DOCKER] Using Java 17 for versions 1.18 to 1.20.4");
                        return "eclipse-temurin:17-jdk";
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not parse version " + mcVersion + " defaulting to Java 25.");
        }
        System.out.println("[DOCKER] Using default Java 25.");
        return "eclipse-temurin:25-jdk";
    }
}
