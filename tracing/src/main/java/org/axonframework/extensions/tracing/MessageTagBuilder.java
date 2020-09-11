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

import io.opentracing.Tracer;
import org.axonframework.messaging.Message;

import java.util.function.BiFunction;

/**
 * @author Corrado Musumeci
 * @author Steven van Beelen
 * @since 4.4
 */
@FunctionalInterface
public interface MessageTagBuilder extends BiFunction<Tracer.SpanBuilder, Message<?>, Tracer.SpanBuilder> {

    /**
     * @param spanBuilder
     * @param message
     * @return
     */
    default Tracer.SpanBuilder build(Tracer.SpanBuilder spanBuilder, Message<?> message) {
        return apply(spanBuilder, message);
    }

    /**
     * @param messageTagBuilder
     * @return
     */
    default MessageTagBuilder with(MessageTagBuilder messageTagBuilder) {
        return (spanBuilder, message) -> {
            Tracer.SpanBuilder first = this.build(spanBuilder, message);
            return messageTagBuilder.build(first, message);
        };
    }

    /**
     * @return
     */
    static MessageTagBuilder noOp() {
        return (spanBuilder, message) -> spanBuilder;
    }
}
