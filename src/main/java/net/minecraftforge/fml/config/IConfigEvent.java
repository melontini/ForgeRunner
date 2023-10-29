/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.config;

import me.melontini.forgerunner.forge.Bindings;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface IConfigEvent {
    record ConfigConfig(Function<ModConfig, IConfigEvent> loading, Function<ModConfig, IConfigEvent> reloading,
                        @Nullable Function<ModConfig, IConfigEvent> unloading) {
    }

    ConfigConfig CONFIGCONFIG = Bindings.getConfigConfiguration().get();

    static IConfigEvent reloading(ModConfig modConfig) {
        return CONFIGCONFIG.reloading().apply(modConfig);
    }

    static IConfigEvent loading(ModConfig modConfig) {
        return CONFIGCONFIG.loading().apply(modConfig);
    }

    @Nullable
    static IConfigEvent unloading(ModConfig modConfig) {
        return CONFIGCONFIG.unloading() == null ? null : CONFIGCONFIG.unloading().apply(modConfig);
    }

    ModConfig getConfig();

    @SuppressWarnings("unchecked")
    default <T extends Event & IConfigEvent> T self() {
        return (T) this;
    }
}
