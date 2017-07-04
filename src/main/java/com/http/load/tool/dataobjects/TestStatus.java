package com.http.load.tool.dataobjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Created by manish kumar.
 */
public class TestStatus {

    private final ConcurrentHashMap<String, AtomicLong> errorsType = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, AtomicLong> non200Responses = new ConcurrentHashMap();

    private final Map<String, AtomicLong> timeTakenPerOperation = new ConcurrentHashMap();
    private final Map<String, AtomicLong> totalRequestsCountPerOperation = new HashMap<>();
    private final Map<String, AtomicLong> openRequestsCountPerOperation = new HashMap<>();

    private AtomicLong openConnections = new AtomicLong(0);
    private AtomicLong errorCount = new AtomicLong(0);
    private AtomicLong successCount = new AtomicLong(0);

    private AtomicLong totalRequests = new AtomicLong(1);
    private AtomicBoolean testInProgress = new AtomicBoolean(false);
    private AtomicBoolean testStarted = new AtomicBoolean(false);
    private AtomicLong maxOpenConnections = new AtomicLong(0);
    private String remoteMethod;
    private long testStartTime = 0;
    private int rampUpTimeCounter = 0;
    private Double rampUpTimeMultiplier = 0D;

    public Double getRampUpTimeMultiplier() {
        return rampUpTimeMultiplier;
    }

    public TestStatus setRampUpTimeMultiplier(Double rampUpTimeMultiplier) {
        this.rampUpTimeMultiplier = rampUpTimeMultiplier;
        return this;
    }

    public int getRampUpTimeCounter() {
        return rampUpTimeCounter;
    }

    public TestStatus setRampUpTimeCounter(int rampUpTimeCounter) {
        this.rampUpTimeCounter = rampUpTimeCounter;
        return this;
    }

    public void incrementRampUpTimeCounter() {
        rampUpTimeCounter++;
    }

    public void decrementRampUpTimeCounter() {
        rampUpTimeCounter--;
    }

    public void markRequestSent(final String operationType) {
        getOpenRequestsCountPerOperation(operationType).incrementAndGet();
        getTotalRequestsCountPerOperation(operationType).incrementAndGet();
        getOpenConnections().incrementAndGet();
        getTotalRequests().incrementAndGet();
    }

    public void markResponseReceived(final String operationType, final long requestSentTime) {
        long timeTaken = System.currentTimeMillis() - requestSentTime;
        getTimeTaken(operationType).addAndGet(timeTaken);
        getOpenRequestsCountPerOperation(operationType).decrementAndGet();
        getOpenConnections().decrementAndGet();
    }

    public AtomicLong getTotalRequestsCountPerOperation(final String operationType) {
        return totalRequestsCountPerOperation.get(operationType);
    }

    public AtomicLong getOpenRequestsCountPerOperation(final String operationType) {
        return openRequestsCountPerOperation.get(operationType);
    }

    public AtomicLong getTimeTaken(final String operationType) {
        AtomicLong time = timeTakenPerOperation.get(operationType);
        if (time == null) {
            time = new AtomicLong(0);
            timeTakenPerOperation.put(operationType, time);
        }
        return time;
    }

    public void reset(final HttpLoadInput httpLoadInput) {
        totalRequestsCountPerOperation.clear();
        openRequestsCountPerOperation.clear();
        httpLoadInput.getRemoteOperations().forEach(remoteOperation -> {
            totalRequestsCountPerOperation.put(remoteOperation.getOperationType(), new AtomicLong(0));
            openRequestsCountPerOperation.put(remoteOperation.getOperationType(), new AtomicLong(0));
        });
        httpLoadInput.getRemoteOperations().forEach(remoteOperation -> {
            remoteOperation.getParameters().forEach(parameter -> {
                parameter.setReplaceValue(parameter.getValue().equalsIgnoreCase("###"));
            });
        });
        httpLoadInput.getCommonParameters().forEach(parameter -> {
            parameter.setReplaceValue(parameter.getValue().equalsIgnoreCase("###"));
        });

        testStartTime = System.currentTimeMillis();
        openConnections.set(0);
        totalRequests.set(1);
        errorCount.set(0);
        successCount.set(0);
        errorsType.clear();
        non200Responses.clear();
    }

    public void shutdown() {
        setTestInProgress(false);
        setTestStarted(false);
        setTestStartTime(0);
        setRampUpTimeMultiplier(0D);
        setRampUpTimeCounter(0);
    }

    public long getTestStartTime() {
        return testStartTime;
    }

    public TestStatus setTestStartTime(long testStartTime) {
        this.testStartTime = testStartTime;
        return this;
    }

    public ConcurrentHashMap<String, AtomicLong> getErrorsType() {
        return errorsType;
    }

    public ConcurrentHashMap<String, AtomicLong> getNon200Responses() {
        return non200Responses;
    }

    public void incrementNon200ResponseCount(final String statusCodeKey) {
        AtomicLong non200Response = getNon200Responses().get(statusCodeKey);
        if (non200Response == null) {
            non200Response = new AtomicLong(0);
        }
        non200Response.incrementAndGet();
        getNon200Responses().put(statusCodeKey, non200Response);
    }

    public AtomicLong getOpenConnections() {
        return openConnections;
    }

    public TestStatus incrementOpenConnections() {
        this.openConnections.incrementAndGet();
        return this;
    }

    public AtomicLong getErrorCount() {
        return errorCount;
    }

    public TestStatus incrementErrorCount() {
        this.errorCount.incrementAndGet();
        return this;
    }

    public TestStatus incrementsSuccessCount() {
        this.successCount.incrementAndGet();
        return this;
    }

    public AtomicLong getTotalRequests() {
        return totalRequests;
    }

    public TestStatus setTotalRequests() {
        this.totalRequests.incrementAndGet();
        return this;
    }

    public boolean isTestNotRunning() {
        return testInProgress.compareAndSet(false, true);
    }

    public TestStatus setTestInProgress(boolean testInProgress) {
        this.testInProgress.set(testInProgress);
        return this;
    }

    public AtomicBoolean getTestStarted() {
        return testStarted;
    }

    public TestStatus setTestStarted(boolean testStarted) {
        this.testStarted.set(testStarted);
        return this;
    }

    public AtomicLong getMaxOpenConnections() {
        return maxOpenConnections;
    }

    public String getRemoteMethod() {
        return remoteMethod;
    }

    public TestStatus setRemoteMethod(String remoteMethod) {
        this.remoteMethod = remoteMethod;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        final List<String> avgTimeForAll = new ArrayList<>();
        totalRequestsCountPerOperation.forEach((operationType, value) -> {
            double avgTime = 0D;
            if (value.get() > 0) {
                avgTime = getTimeTaken(operationType).get() / value.get();
            }
            avgTimeForAll.add(Double.toString(avgTime));
            builder.append(" Avg time for ").append(operationType).append(" is ").append(avgTime).append(".\n");
        });
        totalRequestsCountPerOperation.forEach((operationType, value) -> {
            builder
                    .append(" Total requests for ").append(operationType)
                    .append(" is ").append(value)
                    .append(", open connection count is ").append(getOpenRequestsCountPerOperation(operationType))
                    .append(".\n");
        });

        long testDuration = getTestStartTime() > 0 ? ((System.currentTimeMillis() - getTestStartTime()) / 1000) : 0;

        // Printing some data on Console as well.
        System.out.println("\n\n\n\n Test Duration = " + testDuration + " Seconds"
                + "\n Open connections to remote server = " + getOpenConnections()
                + "\n Total requests = " + (getTotalRequests().get() - 1)
                + "\n Success = " + successCount
                + "\n Error = " + getErrorCount()
                + "\n Error types with count = " + getErrorsType()
                + "\n Non 200 response " + getNon200Responses()
                + "\n " + builder.toString());

        // This is specifically used to generate the data in CSV format that so user can process the log to generate graphs etc.
        return testDuration
                + "," + getOpenConnections()
                + "," + successCount
                + "," + getErrorCount()
                + "," + getTotalRequests()
                + "," + avgTimeForAll.stream().collect(Collectors.joining(","));
    }
}