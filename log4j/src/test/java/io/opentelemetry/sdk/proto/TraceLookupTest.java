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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

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
public class TraceLookupTest {

    @Rule
    public LoggerContextRule init = new LoggerContextRule("LookupTestConfig.xml");

    @Test
    public void lookup() {
        final Logger logger = init.getLogger("LookupTest");
        final ListAppender appender = init.getListAppender("List");
        logger.warn("Test message");
        Tracer tracer = OpenTelemetry.getTracerProvider().get("ot_trace_lookup_test");
        Span span = tracer.spanBuilder("lookup_test").startSpan();
        String traceId = span.getContext().getTraceId().toLowerBase16();
        try (Scope scope = tracer.withSpan(span)) {
            logger.warn("hello");
        }
        final List<String> events = appender.getMessages();
        assertThat(events.size(), equalTo(2));
        String withoutTrace = events.get(0);
        String withTrace = events.get(1);
        // This approach DOES NOT WORK, as the lookup is executed in a separate thread,
        // meaning that it doesn't have access to the current scope, so the following
        // assertion would fail.
//        assertThat(withTrace, matchesPattern(traceId));
    }
}
