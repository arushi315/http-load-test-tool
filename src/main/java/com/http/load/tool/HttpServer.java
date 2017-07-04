package com.http.load.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.http.load.tool.dataobjects.HttpLoadInput;
import com.http.load.tool.dataobjects.TestInput;
import com.http.load.tool.dataobjects.TestStatus;
import com.http.load.tool.executor.HttpLoadExecutor;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Base64;

import static com.http.load.tool.constants.LoadTestType.CONCURRENT_OPEN_CONNECTIONS;
import static com.http.load.tool.constants.LoadTestType.REQUEST_PER_SECOND;
import static io.vertx.core.impl.Arguments.require;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * Created by manish kumar.
 */
@Component
public class HttpServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServer.class);
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private TestStatus testStatus;
    @Autowired
    private TestInput input;
    @Autowired
    private HttpLoadExecutor httpLoadExecutor;

    public void startLoadTest(final RoutingContext context) {
        if (testStatus.isTestNotRunning()) {
            // Not very elegant but for now does the job in most possible clean way.
            input.setHttpLoadInput(validateInput(context));
            testStatus.reset(input.getHttpLoadInput());
            httpLoadExecutor.scheduleTest();
            context.response().setChunked(true).write("Load test triggered successfully!!!").end();
        } else {
            context.response().setChunked(true).write("Load test already running!!!").setStatusCode(400).end();
        }
    }

    public void stopLoadTest(final RoutingContext context) {
        testStatus.shutdown();
        httpLoadExecutor.stopTest();
        context.response().setChunked(true).write("Load test stopped successfully!!!").end();
    }

    private HttpLoadInput validateInput(final RoutingContext context) {
        try {
            // TODO - We need a better way of validation.
            HttpLoadInput httpLoadInput = mapper.readValue(context.getBodyAsJson().toString(), HttpLoadInput.class);
            require(httpLoadInput.getDurationInSeconds() != null && httpLoadInput.getDurationInSeconds() > 0,
                    "Duration to run the tests should be defined in minutes using variable 'durationInSeconds'");
            require(httpLoadInput.getRampUpTimeInSeconds() != null &&
                    httpLoadInput.getRampUpTimeInSeconds() > 0, "Provide 'rampUpTimeInSeconds'");
            require(!CollectionUtils.isEmpty(httpLoadInput.getRemoteOperations()),
                    "Define the remote hosts as array with name 'remoteHostsWithPortAndProtocol'");
            require(httpLoadInput.getTestType() == REQUEST_PER_SECOND || httpLoadInput.getTestType() == CONCURRENT_OPEN_CONNECTIONS,
                    "Define 'testType' parameter value as 'REQUEST_PER_SECOND' or 'CONCURRENT_OPEN_CONNECTIONS'");

            if (httpLoadInput.getTestType() == CONCURRENT_OPEN_CONNECTIONS) {
                require(httpLoadInput.getMaxOpenConnections() > 0, "Define 'maxOpenConnections'");
                httpLoadInput.getRemoteOperations().forEach(remoteOperation -> {
                    require(isNotEmpty(remoteOperation.getOperationType()),
                            "Please define operation type. It can be any string and just used for grouping and reporting purpose.");
                    require(remoteOperation.getLoadPercentage() > 0,
                            "Please Define 'loadPercentage' for " + remoteOperation.getOperationType());
                });
            }
            if (httpLoadInput.isUseBasicAuth()) {
                // For optimization purpose, create Basic Auth header now itself.
                require(httpLoadInput.getBasicAuthUser() != null, "Please provide user for basic auth with key \"basicAuthUser\"");
                require(httpLoadInput.getBasicAuthPassword() != null, "Please provide user for basic auth with key \"basicAuthPassword\"");
                httpLoadInput.setBasicAuthHeader("Basic " + Base64.getEncoder().encodeToString((
                        httpLoadInput.getBasicAuthUser() + ":" + httpLoadInput.getBasicAuthPassword()).getBytes()));
            }
            return httpLoadInput;
        } catch (final IOException e) {
            LOGGER.error("Error parsing test input data.", e);
            testStatus.shutdown();
            httpLoadExecutor.stopTest();
            throw new RuntimeException(e);
        }
    }
}