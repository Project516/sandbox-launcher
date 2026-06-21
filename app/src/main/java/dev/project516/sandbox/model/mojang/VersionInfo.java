package dev.project516.sandbox.model.mojang;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VersionInfo(String id, VersionDownloads downloads) {}
