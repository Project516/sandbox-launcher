package dev.project516.sandbox.model;

public record Instance(String name, String mcVersion) {

    @Override
    public String toString() {
        return name + " (MC " + mcVersion + ")";
    }
}
