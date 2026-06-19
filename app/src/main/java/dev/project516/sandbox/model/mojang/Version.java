package dev.project516.sandbox.model.mojang;

public record Version(String id, String type, String url) {

    @Override
    public String toString() {
        return id + " (" + type + ")";
    }
}
