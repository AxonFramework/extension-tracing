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

/*
 * Copyright (c) 2010-2018. Axon Framework
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.axonframework.messaging.*;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.queryhandling.QueryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link MessageHandlerInterceptor} which maps the {@link MetaData} to the {@link SpanContext}.
 *
 * @author Christophe Bouhier
 */
public class OpenTraceHandlerInterceptor implements MessageHandlerInterceptor<Message<?>> {

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

        Object proceed;

        try(Scope scope = spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(false)){

            //noinspection unchecked
            unitOfWork.onCleanup(u -> {
                scope.span().finish();
            });

            handleMessage(scope.span(), unitOfWork.getMessage());

            proceed = interceptorChain.proceed();
        }


        return proceed;

    }

    private void handleMessage(Span span, Message message) {

        Class payloadType = message.getPayloadType();
        String messageType = resolveType(message);

        span.setOperationName(String.format("%s %s", messageType, payloadType.getSimpleName()));
        span.setTag("axon.Id", message.getIdentifier());
        span.setTag("axon.payloadType", payloadType.toString());

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
