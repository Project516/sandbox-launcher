package dev.project516.sandbox.model.mojang;

public record Settings(String username) {
    public Settings() {
        this("Steve");
    }
}
