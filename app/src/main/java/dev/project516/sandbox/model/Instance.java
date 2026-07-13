package dev.project516.sandbox.model;

public record Instance(String name, String mcVersion, String iconPath, String modLoader) {

    @Override
    public String toString() {
        return name + " (MC " + mcVersion + ")";
    }
}
