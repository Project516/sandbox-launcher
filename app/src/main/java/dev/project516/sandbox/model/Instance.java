package dev.project516.sandbox.model;

public record Instance(String name, String mcVersion, String iconPath) {

    public Instance(String name, String mcVersion) {
        this(name, mcVersion, null);
    }

    @Override
    public String toString() {
        return name + " (MC " + mcVersion + ")";
    }
}
