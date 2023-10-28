package me.melontini.forgerunner.api.adapt;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface Adapter {

    ExecutorService SERVICE = Executors.newFixedThreadPool(Math.min(Math.max(Runtime.getRuntime().availableProcessors(), 2), 8));

    void adapt(IModFile mod, IEnvironment env);

    long priority();
}
