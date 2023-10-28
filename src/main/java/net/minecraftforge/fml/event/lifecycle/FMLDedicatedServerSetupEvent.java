package net.minecraftforge.fml.event.lifecycle;

import net.minecraftforge.fml.ModContainer;

public class FMLDedicatedServerSetupEvent extends ModLifecycleEvent {

    public FMLDedicatedServerSetupEvent(ModContainer container) {
        super(container);
    }
}
