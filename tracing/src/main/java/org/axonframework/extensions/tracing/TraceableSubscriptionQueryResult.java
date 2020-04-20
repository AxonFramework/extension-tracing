/*
 * Copyright (c) 2010-2020. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    public TraceableSubscriptionQueryResult(SubscriptionQueryResult<I, U> subscriptionQueryResult, Span span) {
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