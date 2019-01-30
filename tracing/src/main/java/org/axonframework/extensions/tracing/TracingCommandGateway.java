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
import io.opentracing.Tracer;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;

import java.util.concurrent.CompletableFuture;

/**
 * A tracing command gateway which activates a calling {@link Span}, when the {@link CompletableFuture} completes.
 *
 * @author Christophe Bouhier
 */
public class TracingCommandGateway extends DefaultCommandGateway {

    private final Tracer tracer;

    public TracingCommandGateway(Builder builder, Tracer tracer) {
        super(builder);
        this.tracer = tracer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> CompletableFuture<R> send(Object command) {
        Span parentSpan = tracer.activeSpan();
        try (Scope scope = tracer.buildSpan("command").startActive(false)) {
            Span span = scope.span();
            return (CompletableFuture<R>) super.send(command)
                    .whenComplete((r, e) -> {
                        span.finish();
                        tracer.scopeManager().activate(parentSpan, false);
                    });
        }
    }
}
