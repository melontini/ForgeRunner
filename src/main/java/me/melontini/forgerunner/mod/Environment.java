package me.melontini.forgerunner.mod;

import me.melontini.forgerunner.loader.remapping.ForgeRunnerRemapper;

import java.util.HashMap;

public record Environment(HashMap<String, ModClass> classPool, ForgeRunnerRemapper frr) {

    public void addClass(ModClass modClass) {
        classPool.put(modClass.name(), modClass);
    }
}
