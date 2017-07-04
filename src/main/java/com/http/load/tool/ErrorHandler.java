package com.http.load.tool;

import com.http.load.tool.dataobjects.TestStatus;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by manish kumar.
 */
@Component
public class ErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandler.class);

    @Autowired
    private TestStatus testStatus;

    public void handle(final RoutingContext context) {
        Throwable exception = ((io.vertx.ext.web.RoutingContext) context.getDelegate()).failure();
        LOGGER.error("Unable to handle request {}", context.getBodyAsString(), exception);
        testStatus.shutdown();
        context.response().setChunked(true).write("Error : " + exception.getMessage()).end();
    }
}