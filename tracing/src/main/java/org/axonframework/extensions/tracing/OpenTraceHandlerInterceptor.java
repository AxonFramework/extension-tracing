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
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.queryhandling.QueryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link MessageHandlerInterceptor} which maps the {@link MetaData} to the {@link SpanContext}.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class OpenTraceHandlerInterceptor implements MessageHandlerInterceptor<Message<?>> {

    static final String TAG_AXON_PAYLOAD_TYPE = "axon.payload.type";
    static final String TAG_AXON_ID = "axon.id";
    static final String TAG_AXON_MSG_TYPE = "axon.message.type" ;

    private final Logger LOGGER = LoggerFactory.getLogger(OpenTraceHandlerInterceptor.class);

    private final Tracer tracer;

    public OpenTraceHandlerInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Object handle(UnitOfWork unitOfWork, InterceptorChain interceptorChain) throws Exception {

        MetaData metaData = unitOfWork.getMessage().getMetaData();

        String operationName = "Extracting";
        Tracer.SpanBuilder spanBuilder;
        try {

            MapExtractor extractor = new MapExtractor(metaData);
            SpanContext parentSpan = tracer.extract(Format.Builtin.TEXT_MAP, extractor);

            if (parentSpan == null) {
                spanBuilder = tracer.buildSpan(operationName);
            } else {
                spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpan);
            }
        } catch (IllegalArgumentException e) {
            spanBuilder = tracer.buildSpan(operationName);
        }

        try (Scope scope = spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(false)) {
            //noinspection unchecked
            unitOfWork.onCleanup(u -> {
                scope.span().finish();
            });

            handleMessage(scope.span(), unitOfWork.getMessage());
            return interceptorChain.proceed();
        }
    }

    private void handleMessage(Span span, Message message) {

        Class payloadType = message.getPayloadType();
        String messageType = resolveType(message);

        span.setTag(TAG_AXON_ID, message.getIdentifier());
        span.setTag(TAG_AXON_MSG_TYPE, messageType);
        span.setTag(TAG_AXON_PAYLOAD_TYPE, payloadType.getName());

        LOGGER.info("Called: {}", message);
    }

    private String resolveType(Message message) {
        Class<?> clazz = Message.class;
        if (message instanceof QueryMessage) {
            clazz = QueryMessage.class;
        } else if (message instanceof CommandMessage) {
            clazz = CommandMessage.class;
        } else if (message instanceof EventMessage) {
            clazz = EventMessage.class;
        }
        return clazz.getSimpleName();
    }
}
