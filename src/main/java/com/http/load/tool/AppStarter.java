package com.http.load.tool;

import com.http.load.tool.dataobjects.TestInput;
import com.http.load.tool.dataobjects.TestStatus;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertxbeans.rxjava.VertxBeans;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

/**
 * Created by manish kumar.
 */
@SpringBootApplication
@Import(VertxBeans.class)
public class AppStarter {

    @Autowired
    private Vertx vertx;
    @Autowired
    private ErrorHandler errorHandler;
    @Autowired
    private HttpServer httpServer;

    public static void main(String[] args) {
        new SpringApplication(AppStarter.class).run(args);
    }

    @Bean
    public TestStatus createStatus() {
        return new TestStatus();
    }

    @Bean
    public TestInput createDefaultInput() {
        return new TestInput();
    }

    @PostConstruct
    public void startServer() {
        System.setProperty("logback.configurationFile", "logback.xml");
        int port = Integer.parseInt(System.getProperty("server.port", "8080"));
        vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true))
                .requestHandler(createRequestHandler()::accept).listen(port);
    }

    private Router createRequestHandler() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create()).failureHandler(context -> errorHandler.handle(context));
        router.post("/load/start").handler(context -> httpServer.startLoadTest(context));
        router.post("/load/stop").handler(context -> httpServer.stopLoadTest(context));
        return router;
    }
}