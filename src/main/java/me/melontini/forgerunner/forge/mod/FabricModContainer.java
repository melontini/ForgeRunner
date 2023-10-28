package me.melontini.forgerunner.forge.mod;

import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.loading.LoadingModList;

public class FabricModContainer extends ModContainer {

    private final Object modInstance = new Object();

    public FabricModContainer(ModContainerImpl delegate) {
        super(LoadingModList.fr$getModFileByDelegate(delegate));
        this.contextExtension = () -> null;
    }

    @Override
    public boolean matches(Object mod) {
        return mod == modInstance;
    }

    @Override
    public Object getMod() {
        return modInstance;
    }
}
