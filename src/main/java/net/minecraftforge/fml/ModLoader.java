package net.minecraftforge.fml;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ModLoader {

    private static ModLoader INSTANCE = new ModLoader();

    public static ModLoader get() {
        return INSTANCE;
    }

    public <T extends Event & IModBusEvent> void runEventGenerator(Function<ModContainer, T> generator) {
        ModList.get().forEachModInOrder(mc -> mc.acceptEvent(generator.apply(mc)));
    }

    public <T extends Event & IModBusEvent> void postEvent(T e) {
        ModList.get().forEachModInOrder(mc -> mc.acceptEvent(e));
    }
    public <T extends Event & IModBusEvent> T postEventWithReturn(T e) {
        ModList.get().forEachModInOrder(mc -> mc.acceptEvent(e));
        return e;
    }
    public <T extends Event & IModBusEvent> void postEventWrapContainerInModOrder(T event) {
        postEventWithWrapInModOrder(event, (mc, e) -> ModLoadingContext.get().setActiveContainer(mc), (mc, e) -> ModLoadingContext.get().setActiveContainer(null));
    }
    public <T extends Event & IModBusEvent> void postEventWithWrapInModOrder(T e, BiConsumer<ModContainer, T> pre, BiConsumer<ModContainer, T> post) {
        ModList.get().forEachModInOrder(mc -> {
            pre.accept(mc, e);
            mc.acceptEvent(e);
            post.accept(mc, e);
        });
    }
}
