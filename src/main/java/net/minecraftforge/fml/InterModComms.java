package net.minecraftforge.fml;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class InterModComms {
    public record IMCMessage(String senderModId, String modId, String method, Supplier<?> messageSupplier) {

        @Deprecated
        public final String getSenderModId() {
            return this.senderModId;
        }

        @Deprecated
        public final String getModId() {
            return this.modId;
        }

        @Deprecated
        public final String getMethod() {
            return this.method;
        }

        @SuppressWarnings("unchecked")
        @Deprecated
        public final <T> Supplier<T> getMessageSupplier() {
            return (Supplier<T>) this.messageSupplier;
        }
    }

    private static ConcurrentMap<String, ConcurrentLinkedQueue<IMCMessage>> containerQueues = new ConcurrentHashMap<>();

    public static boolean sendTo(final String modId, final String method, final Supplier<?> thing) {
        if (!ModList.get().isLoaded(modId)) return false;
        containerQueues.computeIfAbsent(modId, k -> new ConcurrentLinkedQueue<>()).add(new IMCMessage(ModLoadingContext.get().getActiveContainer().getModId(), modId, method, thing));
        return true;
    }

    public static boolean sendTo(final String senderModId, final String modId, final String method, final Supplier<?> thing) {
        if (!ModList.get().isLoaded(modId)) return false;
        containerQueues.computeIfAbsent(modId, k -> new ConcurrentLinkedQueue<>()).add(new IMCMessage(senderModId, modId, method, thing));
        return true;
    }

    public static Stream<IMCMessage> getMessages(final String modId, final Predicate<String> methodMatcher) {
        ConcurrentLinkedQueue<IMCMessage> queue = containerQueues.get(modId);
        if (queue == null) return Stream.empty();
        return StreamSupport.stream(new QueueFilteringSpliterator(queue, methodMatcher), false);
    }

    public static Stream<IMCMessage> getMessages(final String modId) {
        return getMessages(modId, s -> Boolean.TRUE);
    }

    private static class QueueFilteringSpliterator implements Spliterator<IMCMessage> {
        private final ConcurrentLinkedQueue<IMCMessage> queue;
        private final Predicate<String> methodFilter;
        private final Iterator<IMCMessage> iterator;

        public QueueFilteringSpliterator(final ConcurrentLinkedQueue<IMCMessage> queue, final Predicate<String> methodFilter) {
            this.queue = queue;
            this.iterator = queue.iterator();
            this.methodFilter = methodFilter;
        }

        @Override
        public int characteristics() {
            return Spliterator.CONCURRENT | Spliterator.NONNULL | Spliterator.ORDERED;
        }

        @Override
        public long estimateSize() {
            return queue.size();
        }

        @Override
        public boolean tryAdvance(final Consumer<? super IMCMessage> action) {
            IMCMessage next;
            do {
                if (!iterator.hasNext()) {
                    return false;
                }
                next = this.iterator.next();
            }
            while (!methodFilter.test(next.method));
            action.accept(next);
            this.iterator.remove();
            return true;
        }

        @Override
        public Spliterator<IMCMessage> trySplit() {
            return null;
        }

    }
}
