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
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.queryhandling.QueryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static org.axonframework.extensions.tracing.SpanUtils.withMessageTags;


/**
 * A {@link MessageHandlerInterceptor} which maps the {@link MetaData} to the {@link SpanContext}.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class OpenTraceHandlerInterceptor implements MessageHandlerInterceptor<Message<?>> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Tracer tracer;
    private final TracingProperties.OperationNamePrefix spanOperationNamePrefix;

    /**
     * Initialize a {@link MessageHandlerInterceptor} implementation which uses the provided {@link Tracer} to map span
     * information from the {@link Message}'s {@link MetaData} on a {@link SpanContext}.
     *
     * @param tracer the {@link Tracer} used to set a {@link SpanContext} on from a {@link Message}'s {@link MetaData}
     * @param tracingProperties only those applicable to message handling will be used.
     */
    public OpenTraceHandlerInterceptor(Tracer tracer, TracingProperties tracingProperties) {
        this.tracer = tracer;
        this.spanOperationNamePrefix = tracingProperties.getHandle().getOperationNamePrefix();
    }

    @Override
    public Object handle(UnitOfWork unitOfWork, InterceptorChain interceptorChain) throws Exception {
        Message<?> message = unitOfWork.getMessage();
        String operationNamePrefix;
        if (message instanceof CommandMessage) {
            operationNamePrefix = spanOperationNamePrefix.getCommand();
        } else if (message instanceof QueryMessage) {
            operationNamePrefix = spanOperationNamePrefix.getQuery();
        } else {
            //fixme what prefix to use here?
            operationNamePrefix = spanOperationNamePrefix.getCommand();
        }
        String operationName = operationNamePrefix + SpanUtils.messageName(message);

        Tracer.SpanBuilder spanBuilder = getParentSpan(message)
                .map(parentSpan -> tracer.buildSpan(operationName).asChildOf(parentSpan))
                .orElse(tracer.buildSpan(operationName));

        final Span span = withMessageTags(spanBuilder, unitOfWork.getMessage()).withTag(Tags.SPAN_KIND.getKey(),
                                                                                        Tags.SPAN_KIND_SERVER).start();
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
}
