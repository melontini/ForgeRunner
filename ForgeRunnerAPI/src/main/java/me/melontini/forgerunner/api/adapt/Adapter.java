package me.melontini.forgerunner.api.adapt;

public interface Adapter {

    void adapt(IModFile mod, IEnvironment env);

    long priority();
}
