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
import reactor.util.annotation.Nullable;
import reactor.util.context.Context;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;


/**
 * An unbounded Java Lambda adapter to {@link Subscriber}
 *
 * @param <T> the value type
 */
final class LambdaSubscriber<T> implements CoreSubscriber<T> {

    final Consumer<? super T> consumer;
    final Consumer<? super Throwable> errorConsumer;
    final Runnable completeConsumer;
    final Consumer<? super Subscription> subscriptionConsumer;
    final Context initialContext;

    volatile Subscription subscription;
    static final AtomicReferenceFieldUpdater<LambdaSubscriber, Subscription> S =
            AtomicReferenceFieldUpdater.newUpdater(LambdaSubscriber.class,
                    Subscription.class,
                    "subscription");

    /**
     * Create a {@link Subscriber} reacting onNext, onError and onComplete. If no
     * {@code subscriptionConsumer} is provided, the subscriber will automatically request
     * Long.MAX_VALUE in onSubscribe, as well as an initial {@link Context} that will be
     * visible by operators upstream in the chain.
     *
     * @param consumer             A {@link Consumer} with argument onNext data
     * @param errorConsumer        A {@link Consumer} called onError
     * @param completeConsumer     A {@link Runnable} called onComplete with the actual
     *                             context if any
     * @param subscriptionConsumer A {@link Consumer} called with the {@link Subscription}
     *                             to perform initial request, or null to request max
     * @param initialContext       A {@link Context} for this subscriber, or null to use the default
     *                             of an {@link Context#empty() empty Context}.
     */
    LambdaSubscriber(
            @Nullable Consumer<? super T> consumer,
            @Nullable Consumer<? super Throwable> errorConsumer,
            @Nullable Runnable completeConsumer,
            @Nullable Consumer<? super Subscription> subscriptionConsumer,
            @Nullable Context initialContext) {
        this.consumer = consumer;
        this.errorConsumer = errorConsumer;
        this.completeConsumer = completeConsumer;
        this.subscriptionConsumer = subscriptionConsumer;
        this.initialContext = initialContext == null ? Context.empty() : initialContext;
    }

    /**
     * Create a {@link Subscriber} reacting onNext, onError and onComplete. If no
     * {@code subscriptionConsumer} is provided, the subscriber will automatically request
     * Long.MAX_VALUE in onSubscribe, as well as an initial {@link Context} that will be
     * visible by operators upstream in the chain.
     *
     * @param consumer             A {@link Consumer} with argument onNext data
     * @param errorConsumer        A {@link Consumer} called onError
     * @param completeConsumer     A {@link Runnable} called onComplete with the actual
     *                             context if any
     * @param subscriptionConsumer A {@link Consumer} called with the {@link Subscription}
     *                             to perform initial request, or null to request max
     */ //left mainly for the benefit of tests
    LambdaSubscriber(
            @Nullable Consumer<? super T> consumer,
            @Nullable Consumer<? super Throwable> errorConsumer,
            @Nullable Runnable completeConsumer,
            @Nullable Consumer<? super Subscription> subscriptionConsumer) {
        this(consumer, errorConsumer, completeConsumer, subscriptionConsumer, null);
    }

    @Override
    public Context currentContext() {
        return this.initialContext;
    }

    @Override
    public final void onSubscribe(Subscription s) {
        this.subscription = s;
        if (subscriptionConsumer != null) {
            subscriptionConsumer.accept(s);
        } else {
            s.request(Long.MAX_VALUE);
        }
    }

    @Override
    public final void onComplete() {
        if (completeConsumer != null) {
            completeConsumer.run();
        }
    }

    @Override
    public final void onError(Throwable t) {
        if (errorConsumer != null) {
            errorConsumer.accept(t);
        }
    }

    @Override
    public final void onNext(T x) {
        if (consumer != null) {
            consumer.accept(x);
        }
    }
}
