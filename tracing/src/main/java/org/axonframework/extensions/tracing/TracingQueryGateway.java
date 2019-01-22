package org.axonframework.extensions.tracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.DefaultQueryGateway;

import java.util.concurrent.CompletableFuture;

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
