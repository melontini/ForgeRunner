package net.minecraftforge.api.distmarker;

public enum Dist {

    CLIENT,
    DEDICATED_SERVER;

    public boolean isDedicatedServer() {
        return !isClient();
    }

    public boolean isClient() {
        return this == CLIENT;
    }
}
