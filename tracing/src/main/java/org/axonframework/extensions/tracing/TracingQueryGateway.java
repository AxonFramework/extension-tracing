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

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.common.Registration;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.DefaultQueryGateway;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryMessage;
import org.axonframework.queryhandling.SubscriptionQueryBackpressure;
import org.axonframework.queryhandling.SubscriptionQueryResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.axonframework.common.BuilderUtils.assertNonNull;
import static org.axonframework.extensions.tracing.SpanUtils.withQueryMessageTags;

/**
 * A tracing {@link QueryGateway} which activates a calling {@link Span}, when the {@link CompletableFuture} completes.
 * This implementation is a wrapper and as such delegates the actual dispatching of queries to another QueryGateway.
 *
 * @author Christophe Bouhier
 * @author Steven van Beelen
 * @author Lucas Campos
 * @since 4.0
 */
public class TracingQueryGateway implements QueryGateway {

    private final Tracer tracer;
    private final QueryGateway delegate;
    private final MessageTagBuilderService messageTagBuilderService;

    /**
     * Instantiate a Builder to be able to create a {@link TracingQueryGateway}.
     * <p>
     * Either a {@link QueryBus} or {@link QueryGateway} can be provided to be used to delegate the dispatching of
     * queries to. If a QueryBus is provided directly, it will be used to instantiate a {@link DefaultQueryGateway}. A
     * registered QueryGateway will always take precedence over a configured QueryBus.
     * <p>
     * The {@link MessageTagBuilderService} is defaulted to a {@link MessageTagBuilderService#defaultService()}. The
     * {@link Tracer} and delegate {@link QueryGateway} are <b>hard requirements</b> and as such should be provided.
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
        this.messageTagBuilderService = builder.messageTagBuilderService;
    }

    @Override
    public <R, Q> CompletableFuture<R> query(String queryName, Q query, ResponseType<R> responseType) {
        Message<?> queryMessage = GenericMessage.asMessage(query);
        return getWithSpan(
                "query_" + SpanUtils.messageName(query.getClass(), queryName),
                queryMessage,
                queryName,
                (childSpan) -> delegate.query(queryName, queryMessage, responseType)
                                       .whenComplete((r, e) -> {
                                           childSpan.log("resultReceived");
                                           childSpan.finish();
                                       })
        );
    }

    @Override
    public <R, Q> Stream<R> scatterGather(String queryName,
                                          Q query,
                                          ResponseType<R> responseType,
                                          long timeout,
                                          TimeUnit timeUnit) {
        Message<?> queryMessage = GenericMessage.asMessage(query);
        return getWithSpan(
                "scatterGather_" + SpanUtils.messageName(query.getClass(), queryName),
                queryMessage,
                queryName,
                (childSpan) -> delegate.scatterGather(queryName, queryMessage, responseType, timeout, timeUnit)
                                       .onClose(() -> {
                                           childSpan.log("resultReceived");
                                           childSpan.finish();
                                       })
        );
    }

    @Override
    public <Q, I, U> SubscriptionQueryResult<I, U> subscriptionQuery(String queryName,
                                                                     Q query,
                                                                     ResponseType<I> initialResponseType,
                                                                     ResponseType<U> updateResponseType,
                                                                     SubscriptionQueryBackpressure backpressure,
                                                                     int updateBufferSize) {
        Message<?> queryMessage = GenericMessage.asMessage(query);
        return getWithSpan(
                "subscriptionQuery_" + SpanUtils.messageName(query.getClass(), queryName),
                queryMessage,
                queryName,
                (childSpan) -> {
                    SubscriptionQueryResult<I, U> subscriptionQueryResult = delegate.subscriptionQuery(
                            queryName, queryMessage, initialResponseType, updateResponseType, backpressure,
                            updateBufferSize
                    );
                    return new TraceableSubscriptionQueryResult<>(subscriptionQueryResult, childSpan);
                }
        );
    }

    private <T> T getWithSpan(String operation, Message<?> query, String queryName, SpanSupplier<T> supplier) {
        // TODO: 12-10-20 Think of nice way to resolve the Message issue here
//        messageTagBuilderService.withMessageTags(tracer.buildSpan(operation), query)
//                                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
        Tracer.SpanBuilder spanBuilder = withQueryMessageTags(tracer.buildSpan(operation), query, queryName)
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
        final Span childSpan = spanBuilder.start();
        try (Scope ignored = tracer.activateSpan(childSpan)) {
            return supplier.get(childSpan);
        }
    }

    @Override
    public Registration registerDispatchInterceptor(
            MessageDispatchInterceptor<? super QueryMessage<?, ?>> dispatchInterceptor) {
        return delegate.registerDispatchInterceptor(dispatchInterceptor);
    }

    @FunctionalInterface
    private interface SpanSupplier<T> {

        T get(Span childSpan);
    }

    /**
     * Builder class to instantiate a {@link TracingQueryGateway}.
     * <p>
     * Either a {@link QueryBus} or {@link QueryGateway} can be provided to be used to delegate the dispatching of
     * queries to. If a QueryBus is provided directly, it will be used to instantiate a {@link DefaultQueryGateway}. A
     * registered QueryGateway will always take precedence over a configured QueryBus.
     * <p>
     * The {@link MessageTagBuilderService} is defaulted to a {@link MessageTagBuilderService#defaultService()}. The
     * {@link Tracer} and delegate {@link QueryGateway} are <b>hard requirements</b> and as such should be provided.
     */
    public static class Builder {

        private Tracer tracer;
        private QueryBus delegateBus;
        private QueryGateway delegateGateway;
        private MessageTagBuilderService messageTagBuilderService;

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
         * Sets the {@link QueryBus} used to build a {@link DefaultQueryGateway} this tracing-wrapper will delegate the
         * actual sending of queries towards.
         *
         * @param delegateBus the {@link QueryGateway} this tracing-wrapper will delegate the actual sending of queries
         *                    towards
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
         * Sets the {@link MessageTagBuilderService} to be used to add {@link CommandMessage} information as tags to a
         * {@link Span}. Defaults to a {@link MessageTagBuilderService#defaultService()}.
         *
         * @param messageTagBuilderService the {@link MessageTagBuilderService} to be used to add {@link CommandMessage}
         *                                 information as tags to a {@link Span}
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder messageTagBuilderService(MessageTagBuilderService messageTagBuilderService) {
            assertNonNull(messageTagBuilderService, "MessageTagBuilderService may not be null");
            this.messageTagBuilderService = messageTagBuilderService;
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
         * queries. Will either use the registered {@link QueryBus} (through {@link #delegateQueryBus(QueryBus)}) or a
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
