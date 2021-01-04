/*
 * Copyright (c) 2020. Axon Framework
 *
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

package org.axonframework.extension.tracing.example

import org.axonframework.eventhandling.tokenstore.inmemory.InMemoryTokenStore
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Starting point.
 */
fun main(args: Array<String>) {
    SpringApplication.run(TracingAxonExampleApplication::class.java, *args)
}

/**
 * Main application class.
 */
@SpringBootApplication
@EnableScheduling
class TracingAxonExampleApplication {

    /**
     * Configures to use in-memory embedded event store.
     */
    @Bean
    fun eventStore() = EmbeddedEventStore.builder().storageEngine(InMemoryEventStorageEngine()).build()

    /**
     * Configures to us in-memory token store.
     */
    @Bean
    fun tokenStore() = InMemoryTokenStore()

}