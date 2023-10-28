package net.minecraftforge.fml.common;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {

    String value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface EventBusSubscriber {

        Dist[] value() default {Dist.CLIENT, Dist.DEDICATED_SERVER};

        String modid() default "";

        Bus bus() default Bus.FORGE;

        enum Bus {

            FORGE(() -> MinecraftForge.EVENT_BUS),
            MOD(() -> FMLJavaModLoadingContext.get().getModEventBus());

            private final Supplier<IEventBus> busSupplier;

            Bus(final Supplier<IEventBus> eventBusSupplier) {
                this.busSupplier = eventBusSupplier;
            }

            public Supplier<IEventBus> bus() {
                return busSupplier;
            }
        }
    }
}
