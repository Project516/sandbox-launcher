package dev.project516.sandbox.model.mojang;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetIndex(String id, String url, long totalSize) {
}
