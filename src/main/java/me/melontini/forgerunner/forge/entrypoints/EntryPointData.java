package me.melontini.forgerunner.forge.entrypoints;

import net.fabricmc.loader.impl.ModContainerImpl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class EntryPointData {

    private final Map<String, ModContainerImpl> perClass = new LinkedHashMap<>();

    public void add(String entrypoint, ModContainerImpl mod) {
        perClass.put(entrypoint, mod);
    }

    public Map<String, ModContainerImpl> getEntrypoints() {
        return Collections.unmodifiableMap(perClass);
    }
}
