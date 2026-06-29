package dev.project516.sandbox.model;

public record Instance(String name, String mcVersion, String iconPath, String modLoader) {

    public Instance(String name, String mcVersion, String iconPath) {
        this(name, mcVersion, iconPath, "vanilla");
    }

    public Instance(String name, String mcVersion) {
        this(name, mcVersion, null, "vanilla");
    }

    @Override
    public String toString() {
        return name + " (MC " + mcVersion + ")";
    }
}
