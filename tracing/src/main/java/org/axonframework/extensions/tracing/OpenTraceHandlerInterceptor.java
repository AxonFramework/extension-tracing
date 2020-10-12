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
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static org.axonframework.common.BuilderUtils.assertNonNull;

/**
 * A {@link MessageHandlerInterceptor} which maps the {@link MetaData} to the {@link SpanContext}.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class OpenTraceHandlerInterceptor implements MessageHandlerInterceptor<Message<?>> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Tracer tracer;
    private final MessageTagBuilderService messageTagBuilderService;

    /**
     * Instantiate a Builder to be able to create a {@link OpenTraceHandlerInterceptor}.
     * <p>
     * The {@link MessageTagBuilderService} is defaulted to a {@link MessageTagBuilderService#defaultService()}. The
     * {@link Tracer} is a <b>hard requirement</b> and as such should be provided.
     *
     * @return a Builder to be able to create a {@link OpenTraceHandlerInterceptor}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initialize a {@link MessageHandlerInterceptor} implementation which uses the provided {@link Tracer} to map span
     * information from the {@link Message}'s {@link MetaData} on a {@link SpanContext}.
     *
     * @param tracer the {@link Tracer} used to set a {@link SpanContext} on from a {@link Message}'s {@link MetaData}
     * @deprecated in favor of using the {@link Builder}
     */
    @Deprecated
    public OpenTraceHandlerInterceptor(Tracer tracer) {
        this.tracer = tracer;
        this.messageTagBuilderService = MessageTagBuilderService.defaultService();
    }

    /**
     * Instantiate a {@link OpenTraceHandlerInterceptor} based on the configuration contained in the {@link Builder}.
     * <p>
     * Will assert that the {@link Tracer} is not {@code null}, and will throw an {@link AxonConfigurationException} if
     * it is.
     *
     * @param builder the {@link Builder} used to instantiate a {@link OpenTraceHandlerInterceptor} instance
     */
    protected OpenTraceHandlerInterceptor(Builder builder) {
        builder.validate();
        this.tracer = builder.tracer;
        this.messageTagBuilderService = builder.messageTagBuilderService;
    }

    @Override
    public Object handle(UnitOfWork unitOfWork, InterceptorChain interceptorChain) throws Exception {
        Message<?> message = unitOfWork.getMessage();
        String operationName = "handle_" + SpanUtils.messageName(message);

        Tracer.SpanBuilder spanBuilder = getParentSpan(message)
                .map(parentSpan -> tracer.buildSpan(operationName).asChildOf(parentSpan))
                .orElse(tracer.buildSpan(operationName));

        final Span span = messageTagBuilderService.withMessageTags(spanBuilder, unitOfWork.getMessage())
                                                  .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                                                  .start();

        try (Scope ignored = tracer.activateSpan(span)) {
            //noinspection unchecked
            unitOfWork.onCleanup(u -> span.finish());
            return interceptorChain.proceed();
        }
    }

    private Optional<SpanContext> getParentSpan(Message<?> message) {
        MetaData metaData = message.getMetaData();
        MapExtractor extractor = new MapExtractor(metaData);
        try {
            return Optional.ofNullable(tracer.extract(Format.Builtin.TEXT_MAP, extractor));
        } catch (IllegalArgumentException ex) {
            logger.error("Corrupted parent span", ex);
            return Optional.empty();
        }
    }

    /**
     * Builder class to instantiate a {@link OpenTraceHandlerInterceptor}.
     * <p>
     * The {@link MessageTagBuilderService} is defaulted to a {@link MessageTagBuilderService#defaultService()}. The
     * {@link Tracer} is a <b>hard requirement</b> and as such should be provided.
     */
    public static class Builder {

        private Tracer tracer;
        private MessageTagBuilderService messageTagBuilderService = MessageTagBuilderService.defaultService();

        /**
         * Sets the {@link Tracer} used to set a {@link Span} on handled {@link Message}s.
         *
         * @param tracer a {@link Tracer} used to set a {@link Span} on handled {@link Message}s.
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder tracer(Tracer tracer) {
            assertNonNull(tracer, "Tracer may not be null");
            this.tracer = tracer;
            return this;
        }

        /**
         * Sets the {@link MessageTagBuilderService} to be used to add {@link Message} information as tags to a {@link
         * Span}. Defaults to a {@link MessageTagBuilderService#defaultService()}.
         *
         * @param messageTagBuilderService the {@link MessageTagBuilderService} to be used to add {@link Message}
         *                                 information as tags to a {@link Span}
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder messageTagBuilderService(MessageTagBuilderService messageTagBuilderService) {
            assertNonNull(messageTagBuilderService, "MessageTagBuilderService may not be null");
            this.messageTagBuilderService = messageTagBuilderService;
            return this;
        }

        /**
         * Initializes a {@link OpenTraceHandlerInterceptor} as specified through this Builder.
         *
         * @return a {@link OpenTraceHandlerInterceptor} as specified through this Builder
         */
        public OpenTraceHandlerInterceptor build() {
            return new OpenTraceHandlerInterceptor(this);
        }

        /**
         * Validate whether the fields contained in this Builder as set accordingly.
         *
         * @throws AxonConfigurationException if one field is asserted to be incorrect according to the Builder's
         *                                    specifications
         */
        protected void validate() {
            assertNonNull(tracer, "The Tracer is a hard requirement and should be provided");
        }
    }
}
