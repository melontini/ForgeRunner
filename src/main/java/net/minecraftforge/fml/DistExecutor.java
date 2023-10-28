package net.minecraftforge.fml;

import lombok.extern.log4j.Log4j2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Log4j2
public final class DistExecutor {
    private DistExecutor() {
    }

    @Deprecated
    public static <T> T callWhenOn(Dist dist, Supplier<Callable<T>> toRun) {
        return unsafeCallWhenOn(dist, toRun);
    }

    public static <T> T unsafeCallWhenOn(Dist dist, Supplier<Callable<T>> toRun) {
        if (dist == FMLEnvironment.dist) {
            try {
                return toRun.get().call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static <T> T safeCallWhenOn(Dist dist, Supplier<SafeCallable<T>> toRun) {
        validateSafeReferent(toRun);
        return callWhenOn(dist, toRun::get);
    }

    @Deprecated
    public static void runWhenOn(Dist dist, Supplier<Runnable> toRun) {
        unsafeRunWhenOn(dist, toRun);
    }

    public static void unsafeRunWhenOn(Dist dist, Supplier<Runnable> toRun) {
        if (dist == FMLEnvironment.dist) {
            toRun.get().run();
        }
    }

    public static void safeRunWhenOn(Dist dist, Supplier<SafeRunnable> toRun) {
        validateSafeReferent(toRun);
        if (dist == FMLEnvironment.dist) {
            toRun.get().run();
        }
    }

    @Deprecated
    public static <T> T runForDist(Supplier<Supplier<T>> clientTarget, Supplier<Supplier<T>> serverTarget) {
        return unsafeRunForDist(clientTarget, serverTarget);
    }

    public static <T> T unsafeRunForDist(Supplier<Supplier<T>> clientTarget, Supplier<Supplier<T>> serverTarget) {
        return switch (FMLEnvironment.dist) {
            case CLIENT -> clientTarget.get().get();
            case DEDICATED_SERVER -> serverTarget.get().get();
        };
    }

    public static <T> T safeRunForDist(Supplier<SafeSupplier<T>> clientTarget, Supplier<SafeSupplier<T>> serverTarget) {
        validateSafeReferent(clientTarget);
        validateSafeReferent(serverTarget);
        return switch (FMLEnvironment.dist) {
            case CLIENT -> clientTarget.get().get();
            case DEDICATED_SERVER -> serverTarget.get().get();
        };
    }

    public interface SafeReferent {
    }

    public interface SafeCallable<T> extends SafeReferent, Callable<T>, Serializable {
    }

    public interface SafeSupplier<T> extends SafeReferent, Supplier<T>, Serializable {
    }

    public interface SafeRunnable extends SafeReferent, Runnable, Serializable {
    }

    private static void validateSafeReferent(Supplier<? extends SafeReferent> safeReferentSupplier) {
        if (FMLEnvironment.production) return;
        final SafeReferent setter;
        try {
            setter = safeReferentSupplier.get();
        } catch (Exception e) {
            return;
        }

        for (Class<?> cl = setter.getClass(); cl != null; cl = cl.getSuperclass()) {
            try {
                Method m = cl.getDeclaredMethod("writeReplace");
                m.setAccessible(true);
                Object replacement = m.invoke(setter);
                if (!(replacement instanceof SerializedLambda l))
                    break;
                if (Objects.equals(l.getCapturingClass(), l.getImplClass())) {
                    log.fatal("Detected unsafe referent usage, please view the code at {}", Thread.currentThread().getStackTrace()[3]);
                    throw new RuntimeException("Unsafe Referent usage found in safe referent method");
                }
            } catch (NoSuchMethodException e) {
            } catch (IllegalAccessException | InvocationTargetException e) {
                break;
            }
        }
    }
}
