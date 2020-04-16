package org.axonframework.extensions.tracing;

import io.opentracing.Span;
import org.axonframework.common.Registration;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Traceable implementation of {@link SubscriptionQueryResult}.
 *
 * @param <I> The type of initial result
 * @param <U> The type of incremental updates
 * @author Lucas Campos
 * @since 4.3
 */
public class TraceableSubscriptionQueryResult<I, U> implements SubscriptionQueryResult<I, U> {

    private final Mono<I> initialResult;
    private final Flux<U> updates;
    private final Registration registrationDelegate;
    private final Span span;

    /**
     * Initializes a Traceable SubscriptionQueryResult which contains the original subscriptionQueryResult and the
     * responsible Span.
     *
     * @param subscriptionQueryResult the original subscriptionQueryResult
     * @param span                    the span wrapping the subscriptionQuery
     */
    public TraceableSubscriptionQueryResult(SubscriptionQueryResult subscriptionQueryResult, Span span) {
        this.initialResult = subscriptionQueryResult.initialResult();
        this.updates = subscriptionQueryResult.updates();
        this.registrationDelegate = subscriptionQueryResult;
        this.span = span;
    }

    @Override
    public Mono<I> initialResult() {
        span.log("initialResultReceived");
        return initialResult;
    }

    @Override
    public Flux<U> updates() {
        return updates.doOnEach(ignored -> span.log("updateReceived"));
    }

    @Override
    public boolean cancel() {
        span.log("subscriptionClosed");
        span.finish();
        return registrationDelegate.cancel();
    }
}