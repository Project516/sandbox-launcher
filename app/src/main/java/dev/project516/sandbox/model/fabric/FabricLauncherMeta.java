package dev.project516.sandbox.model.fabric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FabricLauncherMeta(
        int version, int minLauncherVersion, String launchType, JsonNode mainClass, FabricLibraries libraries) {}
