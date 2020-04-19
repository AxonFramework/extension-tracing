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
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.CommandResultMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.GenericCommandResultMessage;
import org.axonframework.commandhandling.callbacks.FailureLoggingCallback;
import org.axonframework.commandhandling.callbacks.FutureCallback;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.common.Registration;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.axonframework.common.BuilderUtils.assertNonNull;
import static org.axonframework.extensions.tracing.SpanUtils.withMessageTags;

/**
 * A tracing {@link CommandGateway} which activates a calling {@link Span}, when the {@link CompletableFuture}
 * completes. This implementation is a wrapper and as such delegates the actual dispatching of commands to another
 * CommandGateway.
 *
 * @author Christophe Bouhier
 * @author Allard Buijze
 * @author Steven van Beelen
 * @author Lucas Campos
 * @since 4.0
 */
public class TracingCommandGateway implements CommandGateway {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Tracer tracer;
    private final CommandGateway delegate;

    /**
     * Instantiate a {@link TracingCommandGateway} based on the fields contained in the {@link Builder}.
     * <p>
     * Will assert that the {@link Tracer} and delegate {@link CommandGateway} are not {@code null}, and will throw an
     * {@link AxonConfigurationException} if they are.
     *
     * @param builder the {@link Builder} used to instantiate a {@link TracingCommandGateway} instance
     */
    protected TracingCommandGateway(Builder builder) {
        builder.validate();
        this.tracer = builder.tracer;
        this.delegate = builder.buildDelegateCommandGateway();
    }

    /**
     * Instantiate a Builder to be able to create a {@link TracingCommandGateway}.
     * <p>
     * Either a {@link CommandBus} or {@link CommandGateway} can be provided to be used to delegate the dispatching of
     * commands to. If a CommandBus is provided directly, it will be used to instantiate a {@link
     * DefaultCommandGateway}. A registered CommandGateway will always take precedence over a configured CommandBus.
     * <p>
     * The {@link Tracer} and delegate {@link CommandGateway} are <b>hard requirements</b> and as such should be
     * provided.
     *
     * @return a Builder to be able to create a {@link TracingCommandGateway}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <C, R> void send(C command, CommandCallback<? super C, ? super R> callback) {
        CommandMessage<? super C> cmd = GenericCommandMessage.asCommandMessage(command);
        sendWithSpan("send_" + SpanUtils.messageName(cmd), cmd, span -> {
            CompletableFuture<?> resultReceived = new CompletableFuture<>();
            delegate.send(cmd, (CommandCallback<Object, R>) (commandMessage, commandResultMessage) -> {
                try (Scope ignored = tracer.activateSpan(span)) {
                    span.log("resultReceived");
                    //noinspection unchecked
                    callback.onResult((CommandMessage<? extends C>) commandMessage, commandResultMessage);
                    span.log("afterCallbackInvocation");
                } finally {
                    resultReceived.complete(null);
                }
            });
            span.log("dispatchComplete");
            resultReceived.thenRun(span::finish);
        });
    }

    @Override
    public <R> R sendAndWait(Object command) {
        return doSendAndExtract(command, FutureCallback::getResult);
    }

    @Override
    public <R> R sendAndWait(Object command, long timeout, TimeUnit unit) {
        return doSendAndExtract(command, f -> f.getResult(timeout, unit));
    }

    @Override
    public <R> CompletableFuture<R> send(Object command) {
        FutureCallback<Object, R> callback = new FutureCallback<>();
        send(command, new FailureLoggingCallback<>(logger, callback));
        CompletableFuture<R> result = new CompletableFuture<>();
        callback.exceptionally(GenericCommandResultMessage::asCommandResultMessage)
                .thenAccept(r -> {
                    try {
                        if (r.isExceptional()) {
                            result.completeExceptionally(r.exceptionResult());
                        } else {
                            result.complete(r.getPayload());
                        }
                    } catch (Exception e) {
                        result.completeExceptionally(e);
                    }
                });
        return result;
    }

    private <R> R doSendAndExtract(Object command,
                                   Function<FutureCallback<Object, R>, CommandResultMessage<? extends R>> resultExtractor) {
        FutureCallback<Object, R> futureCallback = new FutureCallback<>();

        CommandMessage<?> cmd = GenericCommandMessage.asCommandMessage(command);
        sendWithSpan("sendAndWait_" + SpanUtils.messageName(cmd), cmd, span -> {
            delegate.send(cmd, futureCallback);
            futureCallback.thenRun(() -> span.log("resultReceived"));

            span.log("dispatchComplete");
            futureCallback.thenRun(span::finish);
        });

        CommandResultMessage<? extends R> commandResultMessage = resultExtractor.apply(futureCallback);
        if (commandResultMessage.isExceptional()) {
            throw asRuntime(commandResultMessage.exceptionResult());
        }
        return commandResultMessage.getPayload();
    }

    private void sendWithSpan(String operation, CommandMessage<?> command, Consumer<Span> spanConsumer) {
        Tracer.SpanBuilder spanBuilder = withMessageTags(tracer.buildSpan(operation), command)
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
        final Span span = spanBuilder.start();
        try (Scope ignored = tracer.activateSpan(span)) {
            spanConsumer.accept(span);
        }
    }

    private RuntimeException asRuntime(Throwable e) {
        if (e instanceof Error) {
            throw (Error) e;
        } else if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new CommandExecutionException("An exception occurred while executing a command", e);
        }
    }

    @Override
    public Registration registerDispatchInterceptor(
            MessageDispatchInterceptor<? super CommandMessage<?>> dispatchInterceptor) {
        return delegate.registerDispatchInterceptor(dispatchInterceptor);
    }

    /**
     * Builder class to instantiate a {@link TracingCommandGateway}.
     * <p>
     * Either a {@link CommandBus} or {@link CommandGateway} can be provided to be used to delegate the dispatching of
     * commands to. If a CommandBus is provided directly, it will be used to instantiate a {@link
     * DefaultCommandGateway}. A registered CommandGateway will always take precedence over a configured CommandBus.
     * <p>
     * The {@link Tracer} and delegate {@link CommandGateway} are <b>hard requirements</b> and as such should be
     * provided.
     */
    public static class Builder {

        private Tracer tracer;
        private CommandBus delegateBus;
        private CommandGateway delegateGateway;

        /**
         * Sets the {@link Tracer} used to set a {@link Span} on dispatched {@link CommandMessage}s.
         *
         * @param tracer a {@link Tracer} used to set a {@link Span} on dispatched {@link CommandMessage}s.
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder tracer(Tracer tracer) {
            assertNonNull(tracer, "Tracer may not be null");
            this.tracer = tracer;
            return this;
        }

        /**
         * Sets the {@link CommandBus} used to build a {@link DefaultCommandGateway} this tracing-wrapper will delegate
         * the actual sending of commands towards.
         *
         * @param delegateBus the {@link CommandGateway} this tracing-wrapper will delegate the actual sending of
         *                    commands towards
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder delegateCommandBus(CommandBus delegateBus) {
            assertNonNull(delegateBus, "Delegate CommandBus may not be null");
            this.delegateBus = delegateBus;
            return this;
        }

        /**
         * Sets the {@link CommandGateway} this tracing-wrapper will delegate the actual sending of commands towards.
         *
         * @param delegateGateway the {@link CommandGateway} this tracing-wrapper will delegate the actual sending of
         *                        commands towards
         * @return the current Builder instance, for fluent interfacing
         */
        public Builder delegateCommandGateway(CommandGateway delegateGateway) {
            assertNonNull(delegateGateway, "Delegate CommandGateway may not be null");
            this.delegateGateway = delegateGateway;
            return this;
        }

        /**
         * Initializes a {@link TracingCommandGateway} as specified through this Builder.
         *
         * @return a {@link TracingCommandGateway} as specified through this Builder
         */
        public TracingCommandGateway build() {
            return new TracingCommandGateway(this);
        }

        /**
         * Instantiate the delegate {@link CommandGateway} this tracing-wrapper gateway will uses to actually dispatch
         * commands. Will either use the registered {@link CommandBus} (through {@link #delegateCommandBus(CommandBus)})
         * or a complete CommandGateway through {@link #delegateCommandGateway(CommandGateway)}.
         *
         * @return the delegate {@link CommandGateway} this tracing-wrapper gateway will uses to actually dispatch
         * commands
         */
        private CommandGateway buildDelegateCommandGateway() {
            return delegateGateway != null
                    ? delegateGateway
                    : DefaultCommandGateway.builder().commandBus(delegateBus).build();
        }

        /**
         * Validate whether the fields contained in this Builder as set accordingly.
         *
         * @throws AxonConfigurationException if one field is asserted to be incorrect according to the Builder's
         *                                    specifications
         */
        protected void validate() throws AxonConfigurationException {
            assertNonNull(tracer, "The Tracer is a hard requirement and should be provided");
            if (delegateBus == null) {
                assertNonNull(
                        delegateGateway, "The delegate CommandGateway is a hard requirement and should be provided"
                );
                return;
            }
            assertNonNull(
                    delegateBus,
                    "The delegate CommandBus is a hard requirement to create a delegate CommandGateway"
                            + " and should be provided"
            );
        }
    }
}
