package me.melontini.forgerunner.forge.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class BusHelper {

    public static void onEventBus(Class<?> cls) {
        Mod.EventBusSubscriber subscriber = cls.getAnnotation(Mod.EventBusSubscriber.class);
        if (subscriber == null) return;
        Mod.EventBusSubscriber.Bus bus = subscriber.bus();
        if (subscriber.value().length > 1) {
            bus.bus().get().register(cls);
        } else {
            Dist dist = subscriber.value()[0];
            if (FMLEnvironment.dist == dist) {
                bus.bus().get().register(cls);
            }
        }
    }
}
