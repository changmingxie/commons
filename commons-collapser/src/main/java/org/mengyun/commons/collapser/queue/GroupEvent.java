package org.mengyun.commons.collapser.queue;

/**
 * Created by changmingxie on 12/2/15.
 */
public class GroupEvent {

    public static final GroupEventFactory FACTORY = new GroupEventFactory();

    private RequestPromise requestPromise;
    private String threadContext;

    public <T,R> void reset(RequestPromise<T, R> requestPromise, String threadContext) {
        this.requestPromise = requestPromise;
        this.threadContext = threadContext;
    }

    public RequestPromise getRequestPromise() {
        return requestPromise;
    }

    public String getThreadContext() {
        return threadContext;
    }

    public void clear() {
        requestPromise = null;
        threadContext = null;
    }
}
