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

import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageDispatchInterceptor;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * A {@link MessageDispatchInterceptor} which maps the {@link SpanContext} to {@link
 * org.axonframework.messaging.MetaData}.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class OpenTraceDispatchInterceptor implements MessageDispatchInterceptor<Message<?>> {

    private final Tracer tracer;

    /**
     * Initialize a {@link MessageDispatchInterceptor} implementation which uses the provided {@link Tracer} to map a
     * {@link SpanContext} on an ingested {@link Message}.
     *
     * @param tracer the {@link Tracer} used to set a {@link SpanContext} on {@link Message}s
     */
    public OpenTraceDispatchInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public BiFunction<Integer, Message<?>, Message<?>> handle(List<? extends Message<?>> messages) {
        ScopeManager scopeManager = tracer.scopeManager();
        Optional<SpanContext> spanContextOptional = Optional.ofNullable(scopeManager)
                                                            .map(ScopeManager::activeSpan)
                                                            .map(Span::context);

        return spanContextOptional.<BiFunction<Integer, Message<?>, Message<?>>>map(spanContext -> (index, message) -> {
            MapInjector injector = new MapInjector();
            tracer.inject(spanContext, Format.Builtin.TEXT_MAP, injector);
            return message.andMetaData(injector.getMetaData());
        }).orElseGet(() -> (i, m) -> m);
    }
}
