package dev.project516.sandbox.model.fabric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FabricVersionInfo(FabricLauncherMeta launcherMeta) {}
