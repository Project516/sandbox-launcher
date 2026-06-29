package dev.project516.sandbox.model.fabric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FabricLauncherMeta(String mainClass, List<FabricLibrary> libraries) {}
