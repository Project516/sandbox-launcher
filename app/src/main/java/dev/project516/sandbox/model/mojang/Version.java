package dev.project516.sandbox.model.mojang;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Version(String id, String type, String url) {

    @Override
    public String toString() {
        return id + " (" + type + ")";
    }
}
