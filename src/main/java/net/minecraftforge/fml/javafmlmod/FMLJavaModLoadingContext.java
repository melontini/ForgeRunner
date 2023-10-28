package net.minecraftforge.fml.javafmlmod;

import lombok.extern.log4j.Log4j2;
import net.minecraftforge.eventbus.EventBusErrorMessage;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.IEventListener;
import net.minecraftforge.fml.ModLoadingContext;

@Log4j2
public class FMLJavaModLoadingContext {

    private final FMLModContainer container;

    public FMLJavaModLoadingContext(FMLModContainer container) {
        this.container = container;
    }

    public IEventBus getModEventBus() {
        return container.getEventBus();
    }

    public static void onEventFailed(IEventBus iEventBus, Event event, IEventListener[] iEventListeners, int i, Throwable throwable) {
        log.error(new EventBusErrorMessage(event, i, iEventListeners, throwable));
    }

    public static FMLJavaModLoadingContext get() {
        return ModLoadingContext.get().extension();
    }
}
