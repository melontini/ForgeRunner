package net.minecraftforge.common;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;

public class MinecraftForge {

    public static final IEventBus EVENT_BUS = BusBuilder.builder().startShutdown().build();

    public static void initialize() {
        FabricLoader.getInstance().invokeEntrypoints("forgerunner:forge-init-hook", Runnable.class, Runnable::run);
    }
}
