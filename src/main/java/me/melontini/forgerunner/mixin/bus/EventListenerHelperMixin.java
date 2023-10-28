package me.melontini.forgerunner.mixin.bus;

import me.melontini.forgerunner.util.Unlocker;
import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventListenerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//Good lord.
//If some mod loads the bus at the wrong time - we are toast
//An alternative is to fork the bus and ship that.
@Mixin(value = EventListenerHelper.class, remap = false)
public class EventListenerHelperMixin {

    @Inject(at = @At(value = "INVOKE", target = "Ljava/lang/Class;getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;"), method = "computeListenerList", cancellable = true)
    private static void computeListenerList(Class<?> eventClass, boolean fromInstanceCall, CallbackInfoReturnable<ListenerList> cir) {
        try {
            Event event = (Event) Unlocker.getUnsafe().allocateInstance(eventClass);
            cir.setReturnValue(event.getListenerList());
        } catch (InstantiationException e) {
            throw new RuntimeException("Error computing listener list for " + eventClass.getName(), e);
        }
    }
}
