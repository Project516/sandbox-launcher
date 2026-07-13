package dev.project516.sandbox.core;

import dev.project516.sandbox.model.Instance;
import dev.project516.sandbox.model.ModdedProfile;
import dev.project516.sandbox.model.mojang.VersionInfo;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Manages launching Minecraft in a sandbox **/
public class SandboxManager {

    private static final String GHCR_PREFIX = "ghcr.io/project516/";

    private static String ensureImageAvailable(String image, Consumer<String> logConsumer) throws Exception {
        String localName = image.startsWith(GHCR_PREFIX) ? image.substring(GHCR_PREFIX.length()) : image;

        ProcessBuilder check = new ProcessBuilder("docker", "image", "inspect", localName);
        check.redirectErrorStream(true);
        Process localCheck = check.start();
        localCheck.waitFor();

        if (localCheck.exitValue() == 0) {
            return localName;
        }

        ProcessBuilder remoteCheck = new ProcessBuilder("docker", "image", "inspect", image);
        remoteCheck.redirectErrorStream(true);
        Process remoteCheckProcess = remoteCheck.start();
        remoteCheckProcess.waitFor();

        if (remoteCheckProcess.exitValue() == 0) {
            return image;
        }

        logConsumer.accept("[DOCKER] Image " + localName + " not found locally. Pulling from GHCR...");
        ProcessBuilder pull = new ProcessBuilder("docker", "pull", image);
        pull.redirectErrorStream(true);
        Process pullProcess = pull.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pullProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logConsumer.accept("[DOCKER] " + line);
            }
        }
        pullProcess.waitFor();
        logConsumer.accept("[DOCKER] Pull complete for " + image);
        return image;
    }

    public static Process linuxLaunchInstanceInDocker(Instance instance, Consumer<String> logConsumer) {
        String osName = System.getProperty("os.name").toLowerCase();
        String mcVersion = instance.mcVersion();
        String assetIndexId = mcVersion;

        try {
            String home = System.getProperty("user.home");
            String instanceDir = home + "/.sandbox-launcher";
            String jarPath = "/app/versions/" + mcVersion + "/" + mcVersion + ".jar";

            Files.createDirectories(Path.of(home, ".sandbox-launcher", "instances", mcVersion));

            List<String> jarPaths = new ArrayList<>();
            Path versionJsonPath = Path.of(home, ".sandbox-launcher", "versions", mcVersion, mcVersion + ".json");

            if (Files.exists(versionJsonPath)) {
                VersionInfo info = DownloadManager.MAPPER.readValue(versionJsonPath.toFile(), VersionInfo.class);

                if (info.libraries() != null) {
                    for (dev.project516.sandbox.model.mojang.Library lib : info.libraries()) {
                        if (lib.downloads() == null || lib.downloads().artifact() == null) continue;
                        String libPath = lib.downloads().artifact().path();
                        jarPaths.add("/app/libraries/" + libPath);
                    }
                }

                if (info.assetIndex() != null) {
                    assetIndexId = info.assetIndex().id();
                }
            } else {
                logConsumer.accept("[DOCKER] WARNING: Could not find version JSON at " + versionJsonPath);
            }

            String classpath = jarPath + ":" + String.join(":", jarPaths);
            String javaImage = getJavaImageForVersion(mcVersion);

            Path localJarPath = Path.of(home, ".sandbox-launcher", "versions", mcVersion, mcVersion + ".jar");
            logConsumer.accept("[DEBUG] Local JAR exists: " + Files.exists(localJarPath) + " (" + localJarPath + ")");

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

            ModdedProfile modded = loadModdedProfile(instance);
            String mainClass;
            List<String> extraCp = List.of();
            List<String> extraArgs = List.of();

            if (modded != null) {
                mainClass = modded.mainClass();
                extraCp = modded.classpath();
                extraArgs = modded.extraArgs() != null ? modded.extraArgs() : List.of();
                logConsumer.accept("[DOCKER] Using mod loader " + modded.loader() + " mainClass=" + mainClass);
                if (modded.loader().equals("forge") || modded.loader().equals("neoforge")) {
                    disableEarlyWindow(mcVersion, logConsumer);
                }
            } else if (isOldVersion) {
                mainClass = "net.minecraft.client.Minecraft";
            } else {
                mainClass = "net.minecraft.client.main.Main";
            }

            if (!extraCp.isEmpty()) {
                List<String> fixedCp = new ArrayList<>();
                for (String cp : extraCp) {
                    if (!cp.startsWith("/")) {
                        fixedCp.add("/app/" + cp);
                    } else {
                        fixedCp.add(cp);
                    }
                }
                classpath = classpath + ":" + String.join(":", fixedCp);
            }

            logConsumer.accept("[DEBUG] Classpath: " + classpath);
            logConsumer.accept("[DOCKER] Launching with image: " + javaImage);

            javaImage = ensureImageAvailable(javaImage, logConsumer);

            List<String> command = new ArrayList<>(List.of("docker", "run", "--rm"));

            if (osName.contains("linux")) {
                String uid = idOf("-u");
                String gid = idOf("-g");
                String runtimeDir = "/run/user/" + uid;

                command.add("--user");
                command.add(uid + ":" + gid);
                for (String groupId : deviceGroupIds()) {
                    command.add("--group-add");
                    command.add(groupId);
                }

                command.addAll(List.of(
                        "-e", "DISPLAY=:0",
                        "-e", "XDG_RUNTIME_DIR=" + runtimeDir,
                        "-e", "PULSE_SERVER=unix:" + runtimeDir + "/pulse/native",
                        "-e", "LIBGL_ALWAYS_SOFTWARE=1",
                        "-v", "/tmp/.X11-unix:/tmp/.X11-unix",
                        "-v", runtimeDir + ":" + runtimeDir,
                        "--device", "/dev/dri",
                        "--device", "/dev/snd",
                        "-v", "/dev/input:/dev/input"));
            }

            command.add("-w");
            command.add("/app/instances/" + mcVersion);

            command.addAll(List.of("-v", instanceDir + ":/app", javaImage, "java"));

            if (!javaImage.contains("sandbox-java8")) {
                command.add("--enable-native-access=ALL-UNNAMED");
            }

            command.add("-Djava.library.path=/app/versions/" + mcVersion + "/natives");

            if (modded != null
                    && (modded.loader().equals("forge") || modded.loader().equals("neoforge"))) {
                command.add("-DlibraryDirectory=/app/libraries");
            }

            if (modded != null && modded.jvmArgs() != null) {
                command.addAll(modded.jvmArgs());
            }

            command.add("-cp");
            command.add(classpath);

            if (isOldVersion) {
                command.addAll(List.of(
                        "-Dhttp.proxyHost=betacraft.uk",
                        "-Dhttp.proxyPort=11702",
                        "-Dminecraft.applet.TargetDirectory=/app/instances/" + mcVersion,
                        mainClass,
                        PlayerManager.getUsername(),
                        "-token",
                        "0"));
            } else {
                command.add(mainClass);

                if (!extraArgs.isEmpty()) {
                    command.addAll(extraArgs);
                }

                command.addAll(List.of(
                        "--version",
                        mcVersion,
                        "--accessToken",
                        "0",
                        "--gameDir",
                        "/app/instances/" + mcVersion,
                        "--assetsDir",
                        "/app/assets",
                        "--assetIndex",
                        assetIndexId,
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
        String image;
        try {
            String[] parts = mcVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int patch = (parts.length >= 3) ? Integer.parseInt(parts[2]) : 0;

                if (major >= 26) {
                    System.out.println("[DOCKER] Using Java 25");
                    image = "sandbox-java25";
                } else if (major == 1) {
                    if (minor <= 16) {
                        System.out.println("[DOCKER] Using Java 8.");
                        image = "sandbox-java8";
                    } else if (minor == 17) {
                        System.out.println("[DOCKER] Using Java 16.");
                        image = "sandbox-java16";
                    } else if (minor >= 18 && minor <= 21) {
                        if ((minor == 20 && patch >= 5) || minor == 21) {
                            System.out.println("[DOCKER] Using Java 21.");
                            image = "sandbox-java21";
                        } else {
                            System.out.println("[DOCKER] Using Java 17.");
                            image = "sandbox-java17";
                        }
                    } else {
                        image = "sandbox-java25";
                    }
                } else {
                    image = "sandbox-java25";
                }
            } else {
                image = "sandbox-java25";
            }
        } catch (Exception e) {
            System.err.println("Could not parse version " + mcVersion + " defaulting to Java 25.");
            image = "sandbox-java25";
        }
        return GHCR_PREFIX + image;
    }

    private static String idOf(String flag) {
        try {
            Process p = new ProcessBuilder("id", flag).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                p.waitFor();
                if (line != null && line.trim().matches("\\d+")) {
                    return line.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "1000";
    }

    private static List<String> deviceGroupIds() {
        List<String> gids = new ArrayList<>();
        try {
            Process p = new ProcessBuilder(
                            "sh", "-c", "stat -c %g /dev/dri/* /dev/snd/* /dev/input/* 2>/dev/null | sort -u")
                    .start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.matches("\\d+") && !line.equals("0")) {
                        gids.add(line);
                    }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {
        }
        return gids;
    }

    private static void disableEarlyWindow(String mcVersion, Consumer<String> logConsumer) {
        Path fmlToml = Path.of(
                System.getProperty("user.home"), ".sandbox-launcher", "instances", mcVersion, "config", "fml.toml");
        try {
            String body = "earlyWindowControl = false" + System.lineSeparator();
            if (Files.exists(fmlToml)) {
                String existing = Files.readString(fmlToml);
                if (existing.contains("earlyWindowControl = false")) {
                    return;
                }
                if (existing.matches("(?s).*earlyWindowControl\\s*=.*")) {
                    body = existing.replaceAll("earlyWindowControl\\s*=\\s*\\S+", "earlyWindowControl = false");
                } else {
                    body = existing + System.lineSeparator() + body;
                }
            } else {
                Files.createDirectories(fmlToml.getParent());
            }
            Files.writeString(fmlToml, body);
            logConsumer.accept("[DOCKER] Disabled FML early loading window for Wayland compatibility");
        } catch (Exception e) {
            logConsumer.accept("[DOCKER] WARNING: could not write " + fmlToml
                    + " (a previous root-owned run may own it). Delete that file or the instance, then relaunch. "
                    + e.getMessage());
        }
    }

    private static ModdedProfile loadModdedProfile(Instance instance) {
        if (instance.modLoader() == null || instance.modLoader().equalsIgnoreCase("vanilla")) {
            return null;
        }
        try {
            Path cand = Path.of(
                    System.getProperty("user.home"),
                    ".sandbox-launcher",
                    "versions",
                    instance.mcVersion(),
                    instance.mcVersion() + "-" + instance.modLoader().toLowerCase() + ".json");
            if (Files.exists(cand)) {
                return DownloadManager.MAPPER.readValue(cand.toFile(), ModdedProfile.class);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
