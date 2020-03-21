/*
 * Copyright (c) 2010-2019. Axon Framework
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

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.common.Registration;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.axonframework.common.BuilderUtils.assertNonNull;

/**
 * A tracing {@link QueryGateway} which activates a calling {@link Span}, when the {@link CompletableFuture} completes.
 * This implementation is a wrapper and as such delegates the actual dispatching of queries to another QueryGateway.
 * <p>
 * Note that this implementation <b>>does not</b> support tracing for calls towards
 * {@link #scatterGather(String, Object, ResponseType, long, TimeUnit)} and
 * {@link #subscriptionQuery(String, Object, ResponseType, ResponseType, SubscriptionQueryBackpressure, int)} yet.
 *
 * @author Christophe Bouhier
 * @author Steven van Beelen
 * @since 4.0
 */
public class TracingQueryGateway implements QueryGateway {

    private final Tracer tracer;
    private final QueryGateway delegate;

    /**
     * Instantiate a Builder to be able to create a {@link TracingQueryGateway}.
     * <p>
     * Either a {@link QueryBus} or {@link QueryGateway} can be provided to be used to delegate the dispatching of
     * queries to. If a QueryBus is provided directly, it will be used to instantiate a {@link DefaultQueryGateway}.
     * A registered QueryGateway will always take precedence over a configured QueryBus.
     * <p>
     * The {@link Tracer} and delegate {@link QueryGateway} are <b>hard requirements</b> and as such should be
     * provided.
     *
     * @return a Builder to be able to create a {@link TracingQueryGateway}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Instantiate a {@link TracingQueryGateway} based on the fields contained in the {@link Builder}.
     * <p>
     * Will assert that the {@link Tracer} and delegate {@link QueryGateway} are not {@code null}, and will throw an
     * {@link AxonConfigurationException} if they are.
     *
     * @param builder the {@link Builder} used to instantiate a {@link TracingQueryGateway} instance
     */
    protected TracingQueryGateway(Builder builder) {
        builder.validate();
        this.tracer = builder.tracer;
        this.delegate = builder.buildDelegateQueryGateway();
    }

    @Override
    public <R, Q> CompletableFuture<R> query(String queryName, Q query, ResponseType<R> responseType) {
        Span parentSpan = tracer.activeSpan();
        try (Scope scope = tracer.buildSpan("send_" + SpanUtils.messageName(query, queryName)).startActive(false)) {
            Span span = scope.span();
            return delegate.query(queryName, query, responseType).whenComplete((r, e) -> {
                span.finish();
                tracer.scopeManager().activate(parentSpan, false);
            });
        }
    }

    @Override
    public <R, Q> Stream<R> scatterGather(String queryName,
                                          Q query,
                                          ResponseType<R> responseType,
                                          long timeout,
                                          TimeUnit timeUnit) {
        return delegate.scatterGather(queryName, query, responseType, timeout, timeUnit);
    }

    @Override
    public <Q, I, U> SubscriptionQueryResult<I, U> subscriptionQuery(String queryName,
                                                                     Q query,
                                                                     ResponseType<I> initialResponseType,
                                                                     ResponseType<U> updateResponseType,
                                                                     SubscriptionQueryBackpressure backpressure,
                                                                     int updateBufferSize) {
        return delegate.subscriptionQuery(
                queryName, query, initialResponseType, updateResponseType, backpressure, updateBufferSize
        );
    }

    @Override
    public Registration registerDispatchInterceptor(
            MessageDispatchInterceptor<? super QueryMessage<?, ?>> dispatchInterceptor) {
        return delegate.registerDispatchInterceptor(dispatchInterceptor);
    }

    /**
     * Builder class to instantiate a {@link TracingQueryGateway}.
     * <p>
     * Either a {@link QueryBus} or {@link QueryGateway} can be provided to be used to delegate the dispatching of
     * queries to. If a QueryBus is provided directly, it will be used to instantiate a {@link DefaultQueryGateway}.
     * A registered QueryGateway will always take precedence over a configured QueryBus.
     * <p>
     * The {@link Tracer} and delegate {@link QueryGateway} are <b>hard requirements</b> and as such should be
     * provided.
     */
    public static class Builder {

        private Tracer tracer;
        private QueryBus delegateBus;
        private QueryGateway delegateGateway;

        /**
         * Sets the {@link Tracer} used to set a {@link Span} on dispatched {@link QueryMessage}s.
         *
         * @param tracer a {@link Tracer} used to set a {@link Span} on dispatched {@link QueryMessage}s.
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder tracer(Tracer tracer) {
            assertNonNull(tracer, "Tracer may not be null");
            this.tracer = tracer;
            return this;
        }

        /**
         * Sets the {@link QueryBus} used to build a {@link DefaultQueryGateway} this tracing-wrapper will delegate
         * the actual sending of queries towards.
         *
         * @param delegateBus the {@link QueryGateway} this tracing-wrapper will delegate the actual sending of
         *                    queries towards
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder delegateQueryBus(QueryBus delegateBus) {
            assertNonNull(delegateBus, "Delegate QueryBus may not be null");
            this.delegateBus = delegateBus;
            return this;
        }

        /**
         * Sets the {@link QueryGateway} this tracing-wrapper will delegate the actual sending of queries towards.
         *
         * @param delegateGateway the {@link QueryGateway} this tracing-wrapper will delegate the actual sending of
         *                        queries towards
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder delegateQueryGateway(QueryGateway delegateGateway) {
            assertNonNull(delegateGateway, "Delegate QueryGateway may not be null");
            this.delegateGateway = delegateGateway;
            return this;
        }

        /**
         * Initializes a {@link TracingQueryGateway} as specified through this Builder.
         *
         * @return a {@link TracingQueryGateway} as specified through this Builder
         */
        public TracingQueryGateway build() {
            return new TracingQueryGateway(this);
        }

        /**
         * Instantiate the delegate {@link QueryGateway} this tracing-wrapper gateway will uses to actually dispatch
         * queries.
         * Will either use the registered {@link QueryBus} (through {@link #delegateQueryBus(QueryBus)}) or a
         * complete QueryGateway through {@link #delegateQueryGateway(QueryGateway)}.
         *
         * @return the delegate {@link QueryGateway} this tracing-wrapper gateway will uses to actually dispatch queries
         */
        private QueryGateway buildDelegateQueryGateway() {
            return delegateGateway != null
                    ? delegateGateway
                    : DefaultQueryGateway.builder().queryBus(delegateBus).build();
        }

        /**
         * Validate whether the fields contained in this Builder as set accordingly.
         *
         * @throws AxonConfigurationException if one field is asserted to be incorrect according to the Builder's
         *                                    specifications
         */
        protected void validate() throws AxonConfigurationException {
            assertNonNull(tracer, "The Tracer is a hard requirement and should be provided");
            if (delegateBus == null) {
                assertNonNull(
                        delegateGateway, "The delegate QueryGateway is a hard requirement and should be provided"
                );
                return;
            }
            assertNonNull(
                    delegateBus,
                    "The delegate QueryBus is a hard requirement to create a delegate QueryGateway"
                            + " and should be provided"
            );
        }
    }
}
