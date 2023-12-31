package org.mengyun.commons.collapser.queue;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.mengyun.commons.collapser.CollapserConfig;
import org.mengyun.commons.collapser.handler.EventProcessThreadFactory;
import org.mengyun.commons.collapser.handler.GroupEventHandler;
import org.mengyun.commons.collapser.handler.GroupEventProcessDefaultExceptionHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by changming.xie on 11/24/17.
 */
public class GroupDisruptor {

    private static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;
    private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;

    private static final String defaultKey = "default";

    private Map<String, Disruptor<GroupEvent>> disruptorMap = new ConcurrentHashMap<String, Disruptor<GroupEvent>>();

    private CollapserConfig collapserConfig;

    public void setGroupConfig(CollapserConfig collapserConfig) {
        this.collapserConfig = collapserConfig;
    }

    public GroupDisruptor() {
    }

    public void ensureStart() {

        String key = defaultKey;
        if (!disruptorMap.containsKey(key)) {
            synchronized (key.intern()) {
                if (!disruptorMap.containsKey(key)) {

                    int ringBufferSize = this.collapserConfig.getRingBufferSize();

                    final ThreadFactory threadFactory = new EventProcessThreadFactory("GroupDisruptorEventProcessThreadFactory", true, Thread.NORM_PRIORITY) {
                        @Override
                        public Thread newThread(final Runnable r) {
                            final Thread result = super.newThread(r);
                            return result;
                        }
                    };

//                    WaitStrategy waitStrategy = new TimeoutBlockingWaitStrategy(10l, TimeUnit.MILLISECONDS);

                    WaitStrategy waitStrategy = new BlockingWaitStrategy();

                    Disruptor<GroupEvent> disruptor = new Disruptor<GroupEvent>(GroupEvent.FACTORY, ringBufferSize, threadFactory, ProducerType.MULTI, waitStrategy);

                    ExceptionHandler<GroupEvent> errorHandler = new GroupEventProcessDefaultExceptionHandler();

                    disruptor.setDefaultExceptionHandler(errorHandler);

                    int workPoolSize = this.collapserConfig.getWorkPoolSize(); //

                    GroupEventHandler[] asyncEventHandlers = new GroupEventHandler[workPoolSize];
                    for (int i = 0; i < workPoolSize; i++) {
                        asyncEventHandlers[i] = new GroupEventHandler(i, workPoolSize, this.collapserConfig);
                    }

                    disruptor.handleEventsWith(asyncEventHandlers);

                    disruptor.start();

                    disruptorMap.put(key, disruptor);
                }
            }
        }
    }

    /**
     * Decreases the reference count. If the reference count reached zero, the Disruptor and its associated thread are
     * shut down and their references set to {@code null}.
     */
    public boolean stop(final long timeout, final TimeUnit timeUnit) {

        Map<String, Disruptor<GroupEvent>> tempDisruptorMap = new ConcurrentHashMap<String, Disruptor<GroupEvent>>();

        tempDisruptorMap.putAll(disruptorMap);

        if (tempDisruptorMap.isEmpty()) {
            return true; // disruptor was already shut down by another thread
        }

        for (Disruptor<GroupEvent> disruptor : disruptorMap.values()) {
            // We must guarantee that publishing to the RingBuffer has stopped before we call disruptor.shutdown().
            disruptor = null; // client code fails with NPE if log after stop. This is by design.
        }

        for (Disruptor<GroupEvent> temp : tempDisruptorMap.values()) {
            // Calling Disruptor.shutdown() will wait until all enqueued events are fully processed,
            // but this waiting happens in a busy-spin. To avoid (postpone) wasting CPU,
            // we sleep in short chunks, up to 10 seconds, waiting for the ringbuffer to drain.
            for (int i = 0; hasBacklog(temp) && i < MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN; i++) {
                try {
                    Thread.sleep(SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS); // give up the CPU for a while
                } catch (final InterruptedException e) { // ignored
                }
            }
            try {
                // busy-spins until all events currently in the disruptor have been processed, or timeout
                temp.shutdown(timeout, timeUnit);
            } catch (final TimeoutException e) {
                temp.halt(); // give up on remaining events, if any
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the specified disruptor still has unprocessed events.
     */
    private boolean hasBacklog(final Disruptor<?> theDisruptor) {
        final RingBuffer<?> ringBuffer = theDisruptor.getRingBuffer();
        return !ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize());
    }

    public boolean tryPublish(GroupEventTranslator eventTranslator) {
        try {
            String key = defaultKey;
            return disruptorMap.get(key).getRingBuffer().tryPublishEvent(eventTranslator);
        } catch (final NullPointerException npe) {
            // LOG4J2-639: catch NPE if disruptor field was set to null in stop()
            return false;
        }
    }

    public void publish(GroupEventTranslator eventTranslator) {
        String key = defaultKey;
        disruptorMap.get(key).getRingBuffer().publishEvent(eventTranslator);
    }
}
