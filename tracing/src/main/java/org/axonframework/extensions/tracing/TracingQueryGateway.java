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
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.DefaultQueryGateway;

import java.util.concurrent.CompletableFuture;

/**
 * A tracing query gateway which activates a calling {@link Span}, when the {@link CompletableFuture} completes.
 *
 * @author Christophe Bouhier
 * @since 4.0
 */
public class TracingQueryGateway extends DefaultQueryGateway {

    private final Tracer tracer;

    public TracingQueryGateway(Builder builder, Tracer tracer) {
        super(builder);
        this.tracer = tracer;
    }

    @Override
    public <R, Q> CompletableFuture<R> query(String queryName, Q query, ResponseType<R> responseType) {
        Span parentSpan = tracer.activeSpan();
        try (Scope scope = tracer.buildSpan("query").startActive(false)) {
            Span span = scope.span();
            return super.query(queryName, query, responseType).whenComplete((r, e) -> {
                span.finish();
                tracer.scopeManager().activate(parentSpan, false);
            });
        }
    }
}
