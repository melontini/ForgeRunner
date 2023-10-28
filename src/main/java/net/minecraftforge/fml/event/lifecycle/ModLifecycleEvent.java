package net.minecraftforge.fml.event.lifecycle;

import lombok.extern.log4j.Log4j2;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Log4j2
public class ModLifecycleEvent extends Event implements IModBusEvent {

    private final ModContainer container;

    public ModLifecycleEvent(ModContainer container) {
        this.container = container;
    }

    public final String description() {
        String cn = getClass().getName();
        return cn.substring(cn.lastIndexOf('.') + 1);
    }

    public Stream<InterModComms.IMCMessage> getIMCStream() {
        return InterModComms.getMessages(this.container.getModId());
    }

    public Stream<InterModComms.IMCMessage> getIMCStream(Predicate<String> methodFilter) {
        return InterModComms.getMessages(this.container.getModId(), methodFilter);
    }

    ModContainer getContainer() {
        return this.container;
    }

    @Override
    public String toString() {
        return description();
    }

    /// Gets transformed on Forge, DIY here.

    @Override
    public boolean isCancelable() {
        return false;
    }

    @Override
    public boolean hasResult() {
        return false;
    }
}
