package org.mengyun.commons.collapser;

import org.mengyun.commons.collapser.queue.GroupDisruptor;
import org.mengyun.commons.collapser.queue.GroupEventTranslator;
import org.mengyun.commons.collapser.queue.RequestPromise;
import org.mengyun.commons.collapser.context.ThreadContextSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class InvokeCollapser {

    private static final Logger logger = LoggerFactory.getLogger(InvokeCollapser.class);

    private final ThreadLocal<GroupEventTranslator> threadLocalTranslator = new ThreadLocal<GroupEventTranslator>();

    private GroupDisruptor groupDisruptor = new GroupDisruptor();

    private CollapserConfig collapserConfig = new CollapserConfig();

    public CollapserConfig getGroupConfig() {
        return collapserConfig;
    }

    public void setGroupConfig(CollapserConfig collapserConfig) {
        this.collapserConfig = collapserConfig;
    }

    public InvokeCollapser() {

    }

    public InvokeCollapser(CollapserConfig collapserConfig) {
        this.collapserConfig = collapserConfig;
    }

    @PostConstruct
    public void init() {
        this.groupDisruptor.setGroupConfig(this.collapserConfig);
        this.groupDisruptor.ensureStart();
    }

    @PreDestroy
    public void close() {
        this.groupDisruptor.stop(this.collapserConfig.getMaxStopWaitTimeoutSecond(), TimeUnit.SECONDS);
    }

    public <T, R> R invoke(String group, T request, Callable<T, R> callable) {

        RequestPromise<T, R> requestPromise = new RequestPromise<T, R>(group, request, callable);

        boolean published = false;
        int attempts = 0;

        try (GroupEventTranslator eventTranslator = getCachedTranslator()) {

            eventTranslator.reset(requestPromise,
                    ThreadContextSynchronizationManager.getThreadContextSynchronization().getCurrentThreadContext());

            while (!(published = this.groupDisruptor.tryPublish(eventTranslator)) && attempts < collapserConfig.getMaxRetryAttempts()) {
                attempts++;
                LockSupport.parkNanos(attempts * collapserConfig.getRetryIntervalMillsecond() * 1000l * 1000l);
            }
        } catch (Exception e) {
            throw new SystemException(e);
        }

        if (!published && attempts >= collapserConfig.getMaxRetryAttempts()) {
            logger.info(String.format("collapser ring buffer is full, eventHandler will be executed according your QueueFullPolicy, %s.%s",
                    "GroupInvoker",
                    "invoke"));
            throw new SystemException(String.format("collapser ring buffer is full, and retry %d times but failed, the request info is: %s", collapserConfig.getMaxRetryAttempts(), String.valueOf(request)));
        }

        long currentTime = System.currentTimeMillis();
        synchronized (requestPromise) {
            do {
                try {
                    requestPromise.wait(collapserConfig.getRequestTimeoutMillisecond());
                } catch (InterruptedException e) {
                    if (!requestPromise.isComplete()) {
                        throw new SystemException(e);
                    }
                }
            } while (!requestPromise.isComplete()
                    && System.currentTimeMillis() - currentTime < collapserConfig.getRequestTimeoutMillisecond());
        }

        if (!requestPromise.isComplete()) {
            throw new SystemException(String.format("Timeout while group invoke! The timeout is %d milliseconds, request is %s", collapserConfig.getRequestTimeoutMillisecond(), String.valueOf(request)));
        }

        if (requestPromise.isSuccess()) {
            return requestPromise.getResponse();
        } else {
            if (requestPromise.getThrowable() != null) {
                throw new SystemException(requestPromise.getThrowable());
            } else {
                throw new SystemException(String.format("Group call failed with no exception! The request is %s", requestPromise.getRequest()));
            }
        }
    }

    private GroupEventTranslator getCachedTranslator() {
        GroupEventTranslator result = threadLocalTranslator.get();
        if (result == null) {
            result = new GroupEventTranslator();
            threadLocalTranslator.set(result);
        }
        return result;
    }

    ;
}
