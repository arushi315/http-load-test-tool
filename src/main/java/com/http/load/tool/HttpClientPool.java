package com.http.load.tool;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by manish kumar.
 */
@Component
public class HttpClientPool {

    private final Map<String, HttpClient> clients = new HashMap<>();
    @Value("${remote.server.timeout.millis:990000}")
    private int remoteServerTimeoutInMillis;
    @Value("${http.connection.pool.size:50000}")
    private int httpConnectionPoolSize;
    @Value("${ignore.ssl.errors:true}")
    private boolean ignoreSslError;
    @Autowired
    private Vertx vertx;

    public HttpClientRequest post(final String remoteHost, final String remotePath) {
        return createClient(remoteHost).post(remotePath);
    }

    private HttpClient createClient(final String remoteHost) {
        HttpClient httpClient = clients.get(remoteHost);
        if (httpClient == null) {
            URI uri = URI.create(remoteHost);
            boolean secure = StringUtils.equalsIgnoreCase(uri.getScheme(), "https");
            HttpClientOptions options = new HttpClientOptions()
                    .setDefaultHost(uri.getHost())
                    .setSsl(secure)
                    .setConnectTimeout(remoteServerTimeoutInMillis)
                    .setMaxPoolSize(httpConnectionPoolSize)
                    .setTryUseCompression(true)
                    .setIdleTimeout(remoteServerTimeoutInMillis + 5000)
                    .setTrustAll(ignoreSslError)
                    .setVerifyHost(!ignoreSslError);
            if (uri.getPort() == -1) {
                options.setDefaultPort(secure ? 443 : 80);
            } else {
                options.setDefaultPort(uri.getPort());
            }
            httpClient = vertx.createHttpClient(options);
            clients.put(remoteHost, httpClient);
        }
        return httpClient;
    }
}