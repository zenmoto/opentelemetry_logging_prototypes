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
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.util.StringMap;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

@Plugin(name="LayoutWrapper", category="Core", elementType="layout")
public class LayoutWrapper<T extends Serializable> implements Layout<T> {
    private final Layout<T> wrapped;

    public LayoutWrapper(Layout<T> wrapped) {
        this.wrapped = wrapped;
    }
    @Override
    public byte[] getFooter() {
        return wrapped.getFooter();
    }

    @Override
    public byte[] getHeader() {
        return wrapped.getHeader();
    }

    @Override
    public byte[] toByteArray(LogEvent event) {
        return wrapped.toByteArray(buildEventWrapper(event));
    }

    @Override
    public T toSerializable(LogEvent event) {
        return wrapped.toSerializable(buildEventWrapper(event));
    }

    @Override
    public String getContentType() {
        return wrapped.getContentType();
    }

    @Override
    public Map<String, String> getContentFormat() {
        return wrapped.getContentFormat();
    }

    @Override
    public void encode(LogEvent source, ByteBufferDestination destination) {
        wrapped.encode(buildEventWrapper(source), destination);
    }

    @PluginFactory
    public static <X extends Serializable> LayoutWrapper<X> build(@PluginElement("Layout") Layout<X> subLayout) {
        return new LayoutWrapper<>(subLayout);
    }

    // Production implementation probably shouldn't use reflection (or at least it should be benchmarked).
    private static MutableLogEvent buildEventWrapper(LogEvent logEvent) {
        Tracer tracer = OpenTelemetry.getTracerProvider().get("ot_logging_prototype");
        SpanContext context = tracer.getCurrentSpan().getContext();
        MutableLogEvent e = new MutableLogEvent();
        JdkMapAdapterStringMap newMDC = new JdkMapAdapterStringMap(logEvent.getContextData().toMap());
        newMDC.putValue("traceid", context.getTraceId().toLowerBase16());
        newMDC.putValue("spanid", context.getSpanId().toLowerBase16());
        e.initFrom(logEvent);
        e.setContextData(newMDC);
        return e;
    }


}
