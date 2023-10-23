package me.melontini.forgerunner.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

//Unused. Saved for later.
public class Unlocker {

    private static final Unsafe UNSAFE = Exceptions.uncheck(() -> {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    });
    private static final MethodHandles.Lookup IMPL_LOOKUP = Exceptions.uncheck(() -> {
        Field f = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        return (MethodHandles.Lookup) UNSAFE.getObject(UNSAFE.staticFieldBase(f), UNSAFE.staticFieldOffset(f));
    });

    public static void addOpens(Module module, String pkg, Module other) {
        Exceptions.uncheck(() -> IMPL_LOOKUP.findVirtual(Module.class, "implAddExportsOrOpens", MethodType.methodType(void.class, String.class, Module.class, boolean.class, boolean.class))
                .invoke(module, pkg, other, true, true));
    }
}
