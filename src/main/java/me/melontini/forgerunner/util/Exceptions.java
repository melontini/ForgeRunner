package me.melontini.forgerunner.util;

import lombok.SneakyThrows;

public class Exceptions {

    public static <T> T illegalState() {
        throw new IllegalStateException();
    }

    public static <T> T illegalState(String msg) {
        throw new IllegalStateException(msg);
    }

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

    public interface ThrowingConsumer<T> {
        void accept(T t) throws Throwable;
    }

    public interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
