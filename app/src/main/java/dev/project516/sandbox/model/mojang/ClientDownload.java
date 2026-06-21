package dev.project516.sandbox.model.mojang;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClientDownload(String url, String sha1, int size) {}
