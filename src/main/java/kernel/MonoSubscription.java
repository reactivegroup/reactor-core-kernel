package kernel;

import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.publisher.InnerProducer;
import reactor.core.publisher.Operators;
import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class MonoSubscription <T>
        implements Fuseable.SynchronousSubscription<T>, InnerProducer<T> {

    final reactor.core.CoreSubscriber<? super T> actual;

    final T value;

    @Nullable
    final String stepName;

    volatile int once;

    ScalarSubscription(reactor.core.CoreSubscriber<? super T> actual, T value) {
        this(actual, value, null);
    }

    ScalarSubscription(reactor.core.CoreSubscriber<? super T> actual, T value, String stepName) {
        this.value = Objects.requireNonNull(value, "value");
        this.actual = Objects.requireNonNull(actual, "actual");
        this.stepName = stepName;
    }

    @Override
    public void cancel() {
        if (once == 0) {
            Operators.onDiscard(value, actual.currentContext());
        }
        ONCE.lazySet(this, 2);
    }

    @Override
    public void clear() {
        if (once == 0) {
            Operators.onDiscard(value, actual.currentContext());
        }
        ONCE.lazySet(this, 1);
    }

    @Override
    public boolean isEmpty() {
        return once != 0;
    }

    @Override
    public CoreSubscriber<? super T> actual() {
        return actual;
    }

    @Override
    @Nullable
    public T poll() {
        if (once == 0) {
            ONCE.lazySet(this, 1);
            return value;
        }
        return null;
    }

    @Override
    @Nullable
    public Object scanUnsafe(Attr key) {
        if (key == Attr.TERMINATED) return once == 1;
        if (key == Attr.CANCELLED) return once == 2;
        if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;

        return InnerProducer.super.scanUnsafe(key);
    }

    @Override
    public void request(long n) {
        if (validate(n)) {
            if (ONCE.compareAndSet(this, 0, 1)) {
                Subscriber<? super T> a = actual;
                a.onNext(value);
                if(once != 2) {
                    a.onComplete();
                }
            }
        }
    }

    @Override
    public int requestFusion(int requestedMode) {
        if ((requestedMode & Fuseable.SYNC) != 0) {
            return Fuseable.SYNC;
        }
        return 0;
    }

    @Override
    public int size() {
        return isEmpty() ? 0 : 1;
    }

    @Override
    public String stepName() {
        return stepName != null ? stepName : ("scalarSubscription(" + value + ")");
    }
 }