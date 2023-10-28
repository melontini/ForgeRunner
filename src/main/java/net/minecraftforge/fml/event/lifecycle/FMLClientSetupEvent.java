package net.minecraftforge.fml.event.lifecycle;

import net.minecraftforge.fml.ModContainer;

public class FMLClientSetupEvent extends ModLifecycleEvent {

    public FMLClientSetupEvent(ModContainer container) {
        super(container);
    }
}
