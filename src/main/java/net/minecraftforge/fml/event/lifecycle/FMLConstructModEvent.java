package net.minecraftforge.fml.event.lifecycle;

import net.minecraftforge.fml.ModContainer;

public class FMLConstructModEvent extends ModLifecycleEvent {

    public FMLConstructModEvent(ModContainer container) {
        super(container);
    }
}
