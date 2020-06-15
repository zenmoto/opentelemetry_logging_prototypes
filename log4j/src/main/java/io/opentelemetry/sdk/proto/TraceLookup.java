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

package io.opentelemetry.sdk.proto;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

@Plugin(name="ot", category="Lookup")
public class TraceLookup implements StrLookup {

    @Override
    public String lookup(String key) {
        Tracer tracer = OpenTelemetry.getTracerProvider().get("ot_logging_prototype");
        Span span = tracer.getCurrentSpan();
        if (span == null) {
            return null;
        }

        SpanContext context = span.getContext();
        if (context == null) {
            return null;
        }

        if ("traceid".equals(key)) {
            return context.getTraceId().toLowerBase16();
        } else if ("spanid".equals(key)) {
            return context.getSpanId().toLowerBase16();
        } else if ("traceflags".equals(key)) {
            return context.getTraceFlags().toLowerBase16();
        } else {
            return null;
        }
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }
}
