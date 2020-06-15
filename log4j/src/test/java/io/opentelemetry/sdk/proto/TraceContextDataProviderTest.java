package io.opentelemetry.sdk.proto;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

/*
 * Copyright 2020, OpenTelemetry Authors
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
public class TraceContextDataProviderTest {

    @Rule
    public LoggerContextRule init = new LoggerContextRule("ContextDataProviderTestConfig.xml");

    @Test
    public void testLayoutWrapperSync() {
        final Logger logger = init.getLogger("SyncDataProviderTest");
        final ListAppender appender = init.getListAppender("SyncList");
        logger.warn("Test message");
        Tracer tracer = OpenTelemetry.getTracerProvider().get("ot_trace_dataprovider_test");
        Span span = tracer.spanBuilder("dataprovider_test").startSpan();
        String traceId = span.getContext().getTraceId().toLowerBase16();
        try (Scope scope = tracer.withSpan(span)) {
            logger.warn("hello");
        }
        final List<String> events = appender.getMessages();
        assertThat(events.size(), equalTo(2));
        String withoutTrace = events.get(0);
        String withTrace = events.get(1);
        assertThat(withTrace, containsString(traceId));
    }

    @Test
    public void testLayoutWrapperAsync() throws InterruptedException {
        final Logger logger = init.getLogger("AsyncContextDataProviderTest");
        final ListAppender appender = init.getListAppender("AsyncList");
        Tracer tracer = OpenTelemetry.getTracerProvider().get("ot_trace_lookup_test");
        Span span = tracer.spanBuilder("lookup_test").startSpan();
        String traceId = span.getContext().getTraceId().toLowerBase16();
        try (Scope scope = tracer.withSpan(span)) {
            logger.warn("hello");
        }
        Thread.sleep(15); // Default wait for log4j is 10ms
        final List<String> events = appender.getMessages();
        assertThat(events.size(), equalTo(1));
        String withTrace = events.get(0);

        // This approach will not work in async mode, so the following assertion fails.
        assertThat(withTrace, containsString(traceId));
    }

}
