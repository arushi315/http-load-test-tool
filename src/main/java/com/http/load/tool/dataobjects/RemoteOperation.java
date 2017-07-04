package com.http.load.tool.dataobjects;

import java.util.List;

/**
 * Created by manish kumar.
 */
public class RemoteOperation {

    private List<Parameter> parameters;

    // Uniquely identify the operation type. It's a unique key to group the various HTTP
    // request types, and control the HTTP request load parameters, and respective reporting.
    private String operationType;
    private int loadPercentage;
    private int loadRequestsPerSecond;

    public List<Parameter> getParameters() {
        return parameters;
    }

    public RemoteOperation setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    public String getOperationType() {
        return operationType;
    }

    public RemoteOperation setOperationType(String operationType) {
        this.operationType = operationType;
        return this;
    }

    public int getLoadPercentage() {
        return loadPercentage;
    }

    public RemoteOperation setLoadPercentage(int loadPercentage) {
        this.loadPercentage = loadPercentage;
        return this;
    }

    public int getLoadRequestsPerSecond() {
        return loadRequestsPerSecond;
    }

    public RemoteOperation setLoadRequestsPerSecond(int loadRequestsPerSecond) {
        this.loadRequestsPerSecond = loadRequestsPerSecond;
        return this;
    }
}