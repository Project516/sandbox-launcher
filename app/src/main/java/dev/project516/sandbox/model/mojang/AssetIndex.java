package dev.project516.sandbox.model.mojang;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetIndex(String id, String url, long totalSize) {}
