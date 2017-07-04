package com.http.load.tool;

import com.http.load.tool.dataobjects.TestStatus;
import io.vertx.rxjava.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by manish kumar.
 */
@Component
public class StatsPrinter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatsPrinter.class);
    @Autowired
    private Vertx vertx;
    @Autowired
    private TestStatus testStatus;

    @PostConstruct
    public void init() {
        vertx.setPeriodic(5000, doNothing -> {
            LOGGER.info(getStats());
        });
    }

    private String getStats() {
        final StringBuilder builder = new StringBuilder();
        final List<String> avgTimeForAll = new ArrayList<>();
        testStatus.getTotalRequestsCountPerOperation().forEach((operationType, value) -> {
            double avgTime = 0D;
            if (value.get() > 0) {
                avgTime = testStatus.getTimeTaken(operationType).get() / value.get();
            }
            avgTimeForAll.add(Double.toString(avgTime));
            builder.append(" Avg time for ").append(operationType).append(" is ").append(avgTime).append(".\n");
        });
        testStatus.getTotalRequestsCountPerOperation().forEach((operationType, value) -> {
            builder
                    .append(" Total requests for ").append(operationType)
                    .append(" is ").append(value)
                    .append(", open connection count is ")
                    .append(testStatus.getOpenRequestsCountPerOperation(operationType))
                    .append(".\n");
        });

        long testDuration = testStatus.getTestStartTime() > 0 ? ((System.currentTimeMillis() - testStatus.getTestStartTime()) / 1000) : 0;

        // Printing some data on Console as well.
        System.out.println("\n\n\n\n Test Duration = " + testDuration + " Seconds"
                + "\n Open connections to remote server = " + testStatus.getOpenConnections()
                + "\n Total requests = " + (testStatus.getTotalRequests().get() - 1)
                + "\n Success = " + testStatus.getSuccessCount()
                + "\n Error = " + testStatus.getErrorCount()
                + "\n Error types with count = " + testStatus.getErrorsType()
                + "\n Non 200 response " + testStatus.getNon200Responses()
                + "\n " + builder.toString());

        // This is specifically used to generate the data in CSV format that so user can process the log to generate graphs etc.
        return testDuration
                + "," + testStatus.getOpenConnections()
                + "," + testStatus.getSuccessCount()
                + "," + testStatus.getErrorCount()
                + "," + testStatus.getTotalRequests()
                + "," + avgTimeForAll.stream().collect(Collectors.joining(","));
    }
}