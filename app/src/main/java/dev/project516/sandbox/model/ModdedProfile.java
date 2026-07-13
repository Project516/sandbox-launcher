package dev.project516.sandbox.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModdedProfile(
        String loader,
        String mcVersion,
        String mainClass,
        List<String> classpath,
        List<String> extraArgs,
        List<String> jvmArgs) {}
