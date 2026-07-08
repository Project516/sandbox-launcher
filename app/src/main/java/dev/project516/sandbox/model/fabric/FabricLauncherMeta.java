package dev.project516.sandbox.model.fabric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FabricLauncherMeta(
        int version,
        int minLauncherVersion,
        String launchType,
        Map<String, String> mainClass,
        FabricLibraries libraries) {}
