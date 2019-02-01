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

import io.opentracing.*;
import io.opentracing.propagation.Format;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MessageDispatchInterceptor;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * A {@link MessageDispatchInterceptor} which maps the {@link SpanContext} to {@link org.axonframework.messaging.MetaData}
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class OpenTraceDispatchInterceptor implements MessageDispatchInterceptor<Message<?>> {

    private final Tracer tracer;

    public OpenTraceDispatchInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public BiFunction<Integer, Message<?>, Message<?>> handle(List<? extends Message<?>> messages) {

        ScopeManager scopeManager = tracer.scopeManager();
        Optional<SpanContext> spanContextOptional = Optional.ofNullable(scopeManager)
                .map(ScopeManager::active)
                .map(Scope::span)
                .map(Span::context);

        if (spanContextOptional.isPresent()) {
            Optional<? extends Message<?>> singleMessage = messages.stream()
                    .map(m -> {
                        MapInjector injector = new MapInjector();
                        tracer.inject(spanContextOptional.get(), Format.Builtin.TEXT_MAP, injector);
                        return m.andMetaData(injector.getMetaData());
                    }).findAny();
            return singleMessage.<BiFunction<Integer, Message<?>, Message<?>>>map(message -> (i, m) -> message).orElseGet(() -> (i, m) -> m);
        } else {
            return (i, m) -> m;
        }
    }
}
