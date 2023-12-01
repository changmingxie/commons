package org.mengyun.commons.collapser;

public class CollapserConfig {
    private long requestTimeoutMillisecond = 2000l;
    private int maxRetryAttempts = 2;
    private int retryIntervalMillsecond = 20;
    private int ringBufferSize = 1024;
    private int workPoolSize = 4;
    private int maxBatchSize = 128;
    private long maxStopWaitTimeoutSecond = 5;

    public long getRequestTimeoutMillisecond() {
        return requestTimeoutMillisecond;
    }

    public void setRequestTimeoutMillisecond(long requestTimeoutMillisecond) {
        this.requestTimeoutMillisecond = requestTimeoutMillisecond;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public int getRetryIntervalMillsecond() {
        return retryIntervalMillsecond;
    }

    public void setRetryIntervalMillsecond(int retryIntervalMillsecond) {
        this.retryIntervalMillsecond = retryIntervalMillsecond;
    }

    public int getRingBufferSize() {
        return ringBufferSize;
    }

    public void setRingBufferSize(int ringBufferSize) {
        this.ringBufferSize = ringBufferSize;
    }

    public int getWorkPoolSize() {
        return workPoolSize;
    }

    public void setWorkPoolSize(int workPoolSize) {
        this.workPoolSize = workPoolSize;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public long getMaxStopWaitTimeoutSecond() {
        return maxStopWaitTimeoutSecond;
    }

    public void setMaxStopWaitTimeoutSecond(long maxStopWaitTimeoutSecond) {
        this.maxStopWaitTimeoutSecond = maxStopWaitTimeoutSecond;
    }
}
