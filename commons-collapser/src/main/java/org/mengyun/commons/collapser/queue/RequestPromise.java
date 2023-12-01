package org.mengyun.commons.collapser.queue;

import org.mengyun.commons.collapser.Callable;

public class RequestPromise<T, R> {

    private String group;
    private T request;
    private Callable<T, R> callable;
    private R response;

    private Throwable throwable;

    private boolean isComplete;
    private boolean success;

    public RequestPromise(String group, T request, Callable<T, R> callable) {
        this.group = group;
        this.request = request;
        this.callable = callable;
    }

    public Callable<T, R> getCallable() {
        return callable;
    }

    public String getGroup() {
        return this.group;
    }


    public T getRequest() {
        return request;
    }

    public void setRequest(T request) {
        this.request = request;
    }

    public R getResponse() {
        return response;
    }

    public void setResponse(R response) {
        this.response = response;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
