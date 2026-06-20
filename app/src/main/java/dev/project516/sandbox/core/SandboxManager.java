package dev.project516.sandbox.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SandboxManager {
    public static void testDocker() {
        try {
            ProcessBuilder builder = new ProcessBuilder("docker", "ps");

            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[DOCKER] " + line);
            }

            int exitCode = process.waitFor();
            System.out.println("[DOCKER] Exited with code: " + line);

            if (exitCode != 0) {
                System.err.println("Docker command failed! Is the Docker daemon running? code");
            }

        } catch (Exception e) {
            System.err.println("Failed to run docker command. Is Docker installed and added to your PATH?");
            e.printStackTrace();
        }
    }

    public static void runTestContainer() {
        try {
            ProcessBuilder builder = new ProcessBuilder("docker", "run", "--rm", "hello-world");
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[DOCKER OUT] " + line);
            }

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.err.println("[DOCKER ERR] " + line);
            }

            int exitCode = process.waitFor();
            System.out.println("[DOCKER] Container finished with code: " + exitCode);
        } catch (Exception e) {
            System.err.println("Failed to run docker container.");
            e.printStackTrace();
        }
    }

    public static void startPersistentContainer() {
        try {
            ProcessBuilder builder = new ProcessBuilder("docker", "run", "-d", "ubuntu", "sleep", "infinity");
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            String containerId = null;
            while ((line = reader.readLine()) != null) {
                System.out.println("[DOCKER] " + line);
                containerId = line.trim();
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && containerId != null) {
                System.out.println("[DOCKER] Successfully started sandbox! Container ID: " + containerId);
                System.out.println("[Docker] Run `docker ps` in your terminal to see it running!");

                System.out.println("[DOCKER] Stopping container.");
                ProcessBuilder stopper = new ProcessBuilder("docker", "stop", containerId);
                stopper.start().waitFor();
                System.out.println("[DOCKER Stopped.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
