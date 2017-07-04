package com.http.load.tool;

import com.http.load.tool.dataobjects.TestStatus;
import io.vertx.rxjava.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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
            LOGGER.info(testStatus.toString());
        });
    }
}
