package me.melontini.forgerunner.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.function.Supplier;

public class Bindings {

    public static Supplier<IConfigEvent.ConfigConfig> getConfigConfiguration() {
        return () -> new IConfigEvent.ConfigConfig(ModConfigEvent.Loading::new, ModConfigEvent.Reloading::new, ModConfigEvent.Unloading::new);
    }

    public static Supplier<IEventBus> getForgeBus() {
        return () -> MinecraftForge.EVENT_BUS;
    }
}
