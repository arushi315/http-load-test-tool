package com.http.load.tool.executor;

import com.http.load.tool.HttpClientPool;
import com.http.load.tool.RxObserver;
import com.http.load.tool.constants.Headers;
import com.http.load.tool.dataobjects.Parameter;
import com.http.load.tool.dataobjects.RemoteOperation;
import com.http.load.tool.dataobjects.TestInput;
import com.http.load.tool.dataobjects.TestStatus;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.http.load.tool.constants.LoadTestType.REQUEST_PER_SECOND;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Created by manish kumar.
 */
@Component
public class HttpLoadExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpLoadExecutor.class);
    private final List<Long> timers = new ArrayList<>();

    @Autowired
    private Vertx vertx;
    @Autowired
    private TestStatus testStatus;
    @Autowired
    private TestInput testInput;
    @Autowired
    private HttpClientPool httpClientPool;
    @Autowired
    private List<Map<String, String>> testData;

    private long timerId = -1L;

    @PostConstruct
    public void init() {
        // Ramp up time counter - set the multiplier to handle the ramp up time.
        final long timerId = vertx.setPeriodic(1000, Void -> {
            if (testStatus.getTestStarted().get()) {
                testStatus.incrementRampUpTimeCounter();
                testStatus.setRampUpTimeMultiplier(testStatus.getRampUpTimeCounter() / testInput.getHttpLoadInput().getRampUpTimeInSeconds());
            }
        });
        vertx.setPeriodic(1000, Void -> {
            if (testStatus.getTestStarted().get() && testStatus.getRampUpTimeCounter() > testInput.getHttpLoadInput().getRampUpTimeInSeconds()) {
                testStatus.setRampUpTimeMultiplier(1D);
                vertx.cancelTimer(timerId);
                testStatus.decrementRampUpTimeCounter();
            }
        });
    }

    public void stopTest() {
        timers.forEach(id -> {
            vertx.cancelTimer(id);
        });
    }

    public void scheduleTest() {
        testStatus.setTestStarted(true);
        if (testInput.getHttpLoadInput().getTestType() == REQUEST_PER_SECOND) {
            scheduleRequestsPerSecondTimer();
        } else {
            scheduleOpenConnectionsBasedTimer();
        }

        vertx.cancelTimer(timerId);
        timerId = vertx.setTimer(testInput.getHttpLoadInput().getDurationInSeconds() * 1000, id -> {
            System.out.println("*************************************************************************************");
            System.out.println("Stopping the test to make any further request!!!");
            System.out.println("*************************************************************************************");
            testStatus.shutdown();
            stopTest();
            System.out.println("\n\n\n Total errors in test execution: \n\n\n" + testStatus.getErrorsType() + "\n\n\n");
        });
    }

    private void scheduleOpenConnectionsBasedTimer() {
        testInput.getHttpLoadInput().getRemoteOperations().forEach(remoteOperation -> {
            IntStream.rangeClosed(1, 3).forEach(value -> {
                timers.add(vertx.setPeriodic(100 * value, id -> {
                    makeConnectionBasedRequest(remoteOperation, value);
                }));
            });
        });
    }

    private void scheduleRequestsPerSecondTimer() {
        testInput.getHttpLoadInput().getRemoteOperations().forEach(remoteOperation -> {
            timers.add(vertx.setPeriodic(1000, doNothing -> {
                for (int index = 0; index < remoteOperation.getLoadRequestsPerSecond() * testStatus.getRampUpTimeMultiplier(); index++) {
                    sendRequestToRemote(remoteOperation);
                }
            }));
        });
    }

    private void makeConnectionBasedRequest(final RemoteOperation remoteOperation, final int value) {
        if (openNewConnections()) {
            for (int index = 0; index < 60 * value * testStatus.getRampUpTimeMultiplier(); index++) {
                if (testStatus.getOpenConnections().get() < testStatus.getMaxOpenConnections().get()) {
                    long percentage = testStatus.getTotalRequestsCountPerOperation(
                            remoteOperation.getOperationType()).get() * 100 / testStatus.getTotalRequests().get();
                    if (percentage < remoteOperation.getLoadPercentage()) {
                        sendRequestToRemote(remoteOperation);
                    }
                }
            }
        }
    }

    private void sendRequestToRemote(final RemoteOperation remoteOperation) {
        testStatus.markRequestSent(remoteOperation.getOperationType());
        String remoteHost = getRemoteHost();
        String remotePath = testInput.getHttpLoadInput().getPath() + "?" + toQueryParams(remoteOperation);
        HttpClientRequest clientRequest = httpClientPool.post(remoteHost, remotePath);
        final long requestSentTime = System.currentTimeMillis();
        clientRequest.toObservable().subscribe(httpClientResponse -> {
            RxObserver.readBody(httpClientResponse).subscribe(buffer -> {
                // Read the full response otherwise it will cause "Cannot assign requested address" error.
            }, ex -> {
                checkResponse(httpClientResponse, remoteHost, remoteOperation, requestSentTime);
            }, () -> {
                checkResponse(httpClientResponse, remoteHost, remoteOperation, requestSentTime);
            });
        }, ex -> {
            testStatus.markResponseReceived(remoteOperation.getOperationType(), requestSentTime);
            LOGGER.error("Error while connecting to remote host", ex);
            countError(ex);
        });
        clientRequest.headers().addAll(Headers.HTTP_HEADERS);
        if (StringUtils.isNotEmpty(testInput.getHttpLoadInput().getBasicAuthHeader())) {
            clientRequest.headers().add("Authorization", testInput.getHttpLoadInput().getBasicAuthHeader());
        }
        clientRequest.end();
    }

    private void checkResponse(final HttpClientResponse response, final String remoteHost,
                               final RemoteOperation remoteOperation, final long requestSentTime) {
        testStatus.markResponseReceived(remoteOperation.getOperationType(), requestSentTime);
        if (response.statusCode() != 200) {
            String statusCodeKey = response.statusCode() + " :: " + remoteHost + " :: " + remoteOperation.getOperationType();
            testStatus.incrementNon200ResponseCount(statusCodeKey);
        } else {
            testStatus.incrementsSuccessCount();
        }
    }

    private void countError(final Throwable ex) {
        String errorMessage = ex.getMessage();
        if (errorMessage == null) {
            errorMessage = ex.getLocalizedMessage();
        }
        AtomicLong errorTypeCount = testStatus.getErrorsType().get(errorMessage);
        if (errorTypeCount == null) {
            errorTypeCount = new AtomicLong(0);
        }
        errorTypeCount.incrementAndGet();
        testStatus.getErrorsType().put(errorMessage, errorTypeCount);
        testStatus.getErrorCount().incrementAndGet();
    }

    private boolean openNewConnections() {
        int maxOpenConnections = (int) (testStatus.getMaxOpenConnections().get() * testStatus.getRampUpTimeMultiplier());
        return testStatus.getTestStarted().get() && testStatus.getOpenConnections().get() < maxOpenConnections;
    }

    private String getRemoteHost() {
        return testInput.getHttpLoadInput().getRemoteHosts().get(
                (int) (Math.random() * ((testInput.getHttpLoadInput().getRemoteHosts().size() - 1) + 1)));
    }

    private String toQueryParams(final RemoteOperation remoteOperation) {
        int random = (int) (Math.random() * ((testData.size() - 1) + 1));
        Map<String, String> data = testData.get(random);
        List<Parameter> parameters = remoteOperation.getParameters();
        String query = toQuery(data, parameters);
        if (!isEmpty(testInput.getHttpLoadInput().getCommonParameters())) {
            query += "&" + toQuery(data, testInput.getHttpLoadInput().getCommonParameters());
        }
        return query;
    }

    private String toQuery(Map<String, String> data, List<Parameter> parameters) {
        return parameters
                .stream()
                .map(parameter -> parameter.getName() + "=" +
                        (parameter.isReplaceValue() ? data.get(parameter.getName().trim()) : parameter.getValue()))
                .collect(Collectors.joining("&"));
    }
}