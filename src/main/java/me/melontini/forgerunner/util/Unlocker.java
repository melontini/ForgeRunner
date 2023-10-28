package me.melontini.forgerunner.util;

import com.google.common.base.Suppliers;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.function.Supplier;

public class Unlocker {

    private static final Supplier<Unsafe> UNSAFE = Suppliers.memoize(() -> Exceptions.uncheck(() -> {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }));
    private static final Supplier<MethodHandles.Lookup> IMPL_LOOKUP = Suppliers.memoize(() -> Exceptions.uncheck(() -> {
        Field f = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        return (MethodHandles.Lookup) getUnsafe().getObject(getUnsafe().staticFieldBase(f), getUnsafe().staticFieldOffset(f));
    }));

    public static void addOpens(Module module, String pkg, Module other) {
        Exceptions.uncheck(() -> IMPL_LOOKUP.get().findVirtual(Module.class, "implAddExportsOrOpens", MethodType.methodType(void.class, String.class, Module.class, boolean.class, boolean.class))
                .invoke(module, pkg, other, true, true));
    }

    public static Unsafe getUnsafe() {
        return UNSAFE.get();
    }
}
