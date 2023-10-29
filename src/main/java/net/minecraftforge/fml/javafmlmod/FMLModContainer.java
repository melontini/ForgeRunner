package net.minecraftforge.fml.javafmlmod;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.loading.LoadingModList;

import java.util.Optional;
import java.util.function.Supplier;

@Log4j2
public class FMLModContainer extends ModContainer {

    @Getter
    private final IEventBus eventBus = BusBuilder.builder().setExceptionHandler(FMLJavaModLoadingContext::onEventFailed).markerType(IModBusEvent.class).build();
    private final Supplier<Object> modInstanceSupplier;
    private Object modInstance;


    public FMLModContainer(ModContainerImpl delegate, Supplier<Object> modInstance) {
        super(LoadingModList.fr$getModFileByDelegate(delegate));
        this.modInstanceSupplier = modInstance;
        final var ext = new FMLJavaModLoadingContext(this);
        this.contextExtension = () -> ext;
        this.configHandler = Optional.of(ce->this.eventBus.post(ce.self()));
    }

    @Override
    public boolean matches(Object mod) {
        return this.modInstance == mod;
    }

    @Override
    public Object getMod() {
        if (modInstance == null) {
            modInstance = modInstanceSupplier.get();
        }
        return modInstance;
    }

    @Override
    protected <T extends Event & IModBusEvent> void acceptEvent(final T e) {
        try {
            log.trace("Firing event for modid {} : {}", this.getModId(), e);
            this.eventBus.post(e);
            log.trace("Fired event for modid {} : {}", this.getModId(), e);
        } catch (Throwable t) {
            log.error("Caught exception during event {} dispatch for modid {}", e, this.getModId(), t);
            throw new RuntimeException("Caught exception during event dispatch for modid " + this.getModId(), t);
        }
    }
}
