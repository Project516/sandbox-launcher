package dev.project516.sandbox.core;

/** Manage Player username **/
public class PlayerManager {

    private static String username = "Steve";

    public static void setUsername(String name) {
        username = name;
    }

    public static String getUsername() {
        return username;
    }
}
