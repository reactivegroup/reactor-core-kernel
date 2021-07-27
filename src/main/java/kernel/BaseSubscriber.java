/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kernel;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;

/**
 * A simple base class for a {@link Subscriber} implementation that lets the user
 * perform a {@link #request(long)} and {@link #cancel()} on it directly. As the targeted
 * use case is to manually handle requests, the {@link #hookOnSubscribe(Subscription)} and
 * {@link #hookOnNext(Object)} hooks are expected to be implemented, but they nonetheless
 * default to an unbounded request at subscription time. If you need to define a {@link Context}
 * for this {@link BaseSubscriber}, simply override its {@link #currentContext()} method.
 * <p>
 * Override the other optional hooks {@link #hookOnComplete()},
 * {@link #hookOnError(Throwable)} and {@link #hookOnCancel()}
 * to customize the base behavior. You also have a termination hook,
 * {@link #hookFinally(SignalType)}.
 * <p>
 * Most of the time, exceptions triggered inside hooks are propagated to
 * {@link #onError(Throwable)} (unless there is a fatal exception). The class is in the
 * {@code reactor.core.publisher} package, as this subscriber is tied to a single
 * {@link org.reactivestreams.Publisher}.
 *
 * @author Simon Baslé
 */
public abstract class BaseSubscriber<T> implements CoreSubscriber<T>, Subscription {

    volatile Subscription subscription;

    @Override
    public final void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public final void onNext(T value) {
    }

    @Override
    public final void onError(Throwable t) {
        throw Exceptions.errorCallbackNotImplemented(t);
    }

    @Override
    public final void onComplete() {
    }

    @Override
    public final void request(long n) {
        Subscription s = this.subscription;
        if (s != null) {
            s.request(n);
        }
    }

    @Override
    public final void cancel() {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
