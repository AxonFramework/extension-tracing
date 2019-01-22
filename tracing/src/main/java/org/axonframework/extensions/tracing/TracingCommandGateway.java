package org.axonframework.extensions.tracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;

import java.util.concurrent.CompletableFuture;

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
