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
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.unitofwork.UnitOfWork;

import static org.axonframework.extensions.tracing.SpanUtils.withMessageTags;


/**
 * A {@link MessageHandlerInterceptor} which maps the {@link MetaData} to the {@link SpanContext}.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class OpenTraceHandlerInterceptor implements MessageHandlerInterceptor<Message<?>> {

    private final Tracer tracer;

    /**
     * Initialize a {@link MessageHandlerInterceptor} implementation which uses the provided {@link Tracer} to map span
     * information from the {@link Message} its {@link MetaData} on a {@link SpanContext}.
     *
     * @param tracer the {@link Tracer} used to set a {@link SpanContext} on from a {@link Message}'s {@link MetaData}
     */
    public OpenTraceHandlerInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Object handle(UnitOfWork unitOfWork, InterceptorChain interceptorChain) throws Exception {
        MetaData metaData = unitOfWork.getMessage().getMetaData();

        String operationName = "handle" + SpanUtils.messageName(unitOfWork.getMessage());
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

        try (Scope scope = withMessageTags(spanBuilder, unitOfWork.getMessage()).withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(false)) {
            //noinspection unchecked
            unitOfWork.onCleanup(u -> scope.span().finish());
            return interceptorChain.proceed();
        }
    }

}
