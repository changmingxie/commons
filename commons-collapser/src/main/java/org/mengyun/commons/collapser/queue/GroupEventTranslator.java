package org.mengyun.commons.collapser.queue;

import com.lmax.disruptor.EventTranslator;

/**
 * Created by changming.xie on 11/24/17.
 */
public class GroupEventTranslator implements EventTranslator<GroupEvent>, AutoCloseable {

    private RequestPromise requestPromise;
    private String threadContext;

    @Override
    public void translateTo(GroupEvent event, long sequence) {
        event.reset(requestPromise, threadContext);
    }

    public <T,R> void reset(RequestPromise<T,R> eventInvokerEntry, String threadContext) {
        this.requestPromise = eventInvokerEntry;
        this.threadContext = threadContext;
    }

    public RequestPromise getRequestPromise() {
        return requestPromise;
    }

    @Override
    public void close() throws Exception {
        this.requestPromise = null;
        this.threadContext = null;
    }
}
