package org.mengyun.commons.collapser.queue;

/**
 * Created by changmingxie on 12/2/15.
 */
public class GroupEvent {

    public static final GroupEventFactory FACTORY = new GroupEventFactory();

    private volatile RequestPromise requestPromise;
    private volatile String threadContext;
    private volatile int ordinal = -1;

    public <T, R> void reset(RequestPromise<T, R> requestPromise, String threadContext) {
        this.requestPromise = requestPromise;
        this.threadContext = threadContext;
        this.ordinal = caculateOrdinal();
    }

    private int caculateOrdinal() {
        int index = -1;
        if (this.requestPromise != null) {
            index = 0;
            String group = this.requestPromise.getGroup();
            if (group != null && group.length() != 0) {
                index = group.hashCode();
                if (index != Integer.MIN_VALUE) {
                    index = index < 0 ? -index : index;
                }
            }
        }

        return index;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public RequestPromise getRequestPromise() {
        return requestPromise;
    }

    public String getThreadContext() {
        return threadContext;
    }

    public void clear() {
        ordinal = -1;
        requestPromise = null;
        threadContext = null;
    }
}
