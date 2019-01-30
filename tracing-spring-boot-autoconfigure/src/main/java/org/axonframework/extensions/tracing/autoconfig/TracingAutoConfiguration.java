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

package org.axonframework.extensions.tracing.autoconfig;

import io.opentracing.Tracer;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.extensions.tracing.OpenTraceDispatchInterceptor;
import org.axonframework.extensions.tracing.OpenTraceHandlerInterceptor;
import org.axonframework.extensions.tracing.TracingCommandGateway;
import org.axonframework.extensions.tracing.TracingQueryGateway;
import org.axonframework.queryhandling.DefaultQueryGateway;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.springboot.autoconfig.EventProcessingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
@AutoConfigureAfter(EventProcessingAutoConfiguration.class)
public class TracingAutoConfiguration {

    @Bean
    public OpenTraceDispatchInterceptor traceDispatchInterceptor(Tracer tracer) {
        return new OpenTraceDispatchInterceptor(tracer);
    }

    @Bean
    public OpenTraceHandlerInterceptor traceHandlerInterceptor(Tracer tracer) {
        return new OpenTraceHandlerInterceptor(tracer);
    }

    @Bean
    @Primary
    public QueryGateway queryGateway(QueryBus queryBus, OpenTraceDispatchInterceptor openTraceDispatchInterceptor, OpenTraceHandlerInterceptor openTraceHandlerInterceptor, Tracer tracer) {

        queryBus.registerHandlerInterceptor(openTraceHandlerInterceptor);

        DefaultQueryGateway.Builder builder = DefaultQueryGateway.builder()
                .queryBus(queryBus)
                .dispatchInterceptors(openTraceDispatchInterceptor);

        return new TracingQueryGateway(builder, tracer);
    }

    @Bean
    @Primary
    public CommandGateway commandGateway(CommandBus commandBus, OpenTraceDispatchInterceptor openTraceDispatchInterceptor, OpenTraceHandlerInterceptor openTraceHandlerInterceptor, Tracer tracer) {

        commandBus.registerHandlerInterceptor(openTraceHandlerInterceptor);

        DefaultCommandGateway.Builder builder = DefaultCommandGateway.builder()
                .commandBus(commandBus)
                .dispatchInterceptors(openTraceDispatchInterceptor);
        return new TracingCommandGateway(builder, tracer);
    }

}
