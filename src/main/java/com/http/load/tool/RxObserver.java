package com.http.load.tool;

import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClientResponse;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by manish kumar.
 */
public final class RxObserver {

    private RxObserver() {
    }

    public static Observable<Buffer> readBody(final HttpClientResponse response) {
        Buffer buffer = Buffer.buffer();
        return Observable.create((Subscriber<? super Buffer> subs) -> {
            response.toObservable()
                    .subscribe(buffer::appendBuffer,
                            subs::onError,
                            () -> {
                                subs.onNext(buffer);
                                subs.onCompleted();
                            });
        });
    }
}