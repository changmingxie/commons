package org.mengyun.commons.collapser.context;

public interface ThreadContextSynchronization {

    public String getCurrentThreadContext();

    public void setThreadContext(String threadContext);

    public void clear();
}
