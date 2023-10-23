package me.melontini.forgerunner.util;

import lombok.SneakyThrows;

public class Exceptions {

    @SneakyThrows
    public static <T> T uncheck(ThrowingSupplier<T> supplier) {
        return supplier.get();
    }

    @SneakyThrows
    public static void uncheck(ThrowingRunnable runnable) {
        runnable.run();
    }

    public interface ThrowingSupplier<T> {
        T get() throws Throwable;
    }

    public interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
