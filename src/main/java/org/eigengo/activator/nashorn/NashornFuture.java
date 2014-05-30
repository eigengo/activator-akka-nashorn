package org.eigengo.activator.nashorn;

import akka.japi.JavaPartialFunction;
import scala.Tuple2;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Future$;
import scala.util.Try;

import java.util.function.Function;

public class NashornFuture<A> {
    private final Future<A> future;

    public NashornFuture(Future<A> future) {
        this.future = future;
    }

    public static NashornFuture<?> failed(Throwable cause) {
        return new NashornFuture<>(Future$.MODULE$.failed(cause));
    }

    public <U> NashornFuture<U> flatMap(final Function<A, NashornFuture<U>> f, final ExecutionContext executor) {
        return new NashornFuture<>(this.future.flatMap(new JavaPartialFunction<A, Future<U>>() {
            @Override
            public Future<U> apply(A x, boolean isCheck) throws Exception {
                return f.apply(x).future;
            }
        }, executor));
    }

    public <U> NashornFuture<Tuple2<A, U>> zip(final NashornFuture<U> that) {
        return new NashornFuture<>(this.future.zip(that.future));
    }

    public void onComplete(final Function<Try<A>, ?> f, final ExecutionContext executor) {
        this.future.onComplete(new JavaPartialFunction<Try<A>, Object>() {
            @Override
            public Object apply(Try<A> x, boolean isCheck) throws Exception {
                f.apply(x);
                return null;
            }
        }, executor);
    }
}
