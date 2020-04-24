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

/**
 * Properties for tracing customizations.
 */
public class TracingProperties {

    //fixme which coordinates are better?
    //axon.tracing.{dispatch/handle}.{command/query}
    //                 vs
    //axon.tracing.{command/query}.{dispatch/handle}

    public TracingProperties() {
        setDefaults();
    }

    private Handle handle = new Handle();

    private Dispatch dispatch = new Dispatch();

    public Handle getHandle() {
        return handle;
    }

    public void setHandle(Handle handle) {
        this.handle = handle;
    }

    public Dispatch getDispatch() {
        return dispatch;
    }

    public void setDispatch(Dispatch dispatch) {
        this.dispatch = dispatch;
    }

    private void setDefaults() {
        dispatch.getOperationNamePrefix().setCommand("fire_");
        dispatch.getOperationNamePrefix().setQuery("ask_");
        handle.getOperationNamePrefix().setCommand("handle_");
        handle.getOperationNamePrefix().setQuery("serve_");
    }

    private static abstract class HasOperationNamePrefix {

        private OperationNamePrefix operationNamePrefix = new OperationNamePrefix();

        public OperationNamePrefix getOperationNamePrefix() {
            return operationNamePrefix;
        }

        public void setOperationNamePrefix(OperationNamePrefix operationNamePrefix) {
            this.operationNamePrefix = operationNamePrefix;
        }
    }

    /**
     * Customizations for message dispatching.
     */
    public static class Dispatch extends HasOperationNamePrefix {
    }

    /**
     * Customizations for message dispatching.
     */
    public static class Handle extends HasOperationNamePrefix {

    }

    /**
     * Span operation name for messages.
     */
    public static class OperationNamePrefix {

        /**
         * Span operation name prefix for commands.
         * <p/>
         * E.g. given it's {@code "send_"}, the name would be {@code "send_MyCommand"}
         */
        private String command;

        /**
         * Span operation name prefix for queries.
         * <p/>
         * E.g. given it's {@code "send_"}, the name would be {@code "send_MyQuery"}
         */
        private String query;

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }
}
