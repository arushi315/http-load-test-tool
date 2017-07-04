package com.http.load.tool.constants;

import io.vertx.rxjava.core.MultiMap;

/**
 * Created by manish kumar.
 */
public final class Headers {

    // TODO - Allow users to define the custom headers.
    public static MultiMap HTTP_HEADERS = MultiMap.caseInsensitiveMultiMap();

    static {
        HTTP_HEADERS.add("Host", "1.1.1.1:443");
        HTTP_HEADERS.add("X-MS-PolicyKey", "3737366238");
        HTTP_HEADERS.add("Content-Type", "application/vnd.ms-sync.wbxml");
        HTTP_HEADERS.add("Accept", "*/*");
        HTTP_HEADERS.add("Content-Length", "0");
        HTTP_HEADERS.add("Accept-Language", "en-us");
        HTTP_HEADERS.add("Accept-Encoding", "gzip, deflate");
        HTTP_HEADERS.add("MS-ASProtocolVersion", "14.1");
        HTTP_HEADERS.add("User-Agent", "Apple-Device/182.22");
    }
}