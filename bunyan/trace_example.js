const { NodeTracerProvider } = require("@opentelemetry/node");
const { SimpleSpanProcessor } = require("@opentelemetry/tracing");
const { JaegerExporter } = require("@opentelemetry/exporter-jaeger");
const api = require("@opentelemetry/api");
const Logger = require('bunyan');
const tracerProvider = new NodeTracerProvider();

/**
 * The SimpleSpanProcessor does no batching and exports spans
 * immediately when they end. For most production use cases,
 * OpenTelemetry recommends use of the BatchSpanProcessor.
 */
tracerProvider.addSpanProcessor(
    new SimpleSpanProcessor(
        new JaegerExporter({
            serviceName: 'my-service'
        })
    )
);

const logCalls = new Set(['debug', 'info', 'warn', 'error'])
function wrapBunyan(logger) {
    return new Proxy(logger, {
        get: function(obj, prop, receiver) {
            const originalCall = Reflect.get(...arguments);
            if (logCalls.has(prop)) {
                return function () {
                    const span = api.trace.getTracer("test", "123").getCurrentSpan();
                    let args = new Array(...arguments);
                    const frontArg = args[0];
                    if (span && span.isRecording()) {
                        if (prop == "error") {
                            const eventArg = typeof frontArg == "string" ? {msg: frontArg} : frontArg;
                            span.addEvent("message", eventArg)
                        }
                        const ctx = span.context();
                        const toAdd = {
                            "traceid": ctx.traceId,
                            "spanid": ctx.spanId,
                            "traceflags": ctx.traceFlags
                        };
                        if (typeof frontArg == 'string') {
                            args.unshift(toAdd);
                        } else if (typeof frontArg == 'object') {
                            Object.assign(frontArg, toAdd);
                        }
                    }
                    originalCall.apply(obj, args)
                }
            } else if (prop == "child") {
                return function() {
                    return wrapBunyan(originalCall.apply(obj, arguments));
                }
            } else {
                return originalCall;
            }
        }
    });
}

/**
 * Registering the provider with the API allows it to be discovered
 * and used by instrumentation libraries. The OpenTelemetry API provides
 * methods to set global SDK implementations, but the default SDK provides
 * a convenience method named `register` which registers same defaults
 * for you.
 *
 * By default the NodeTracerProvider uses Trace Context for propagation
 * and AsyncHooksScopeManager for context management. To learn about
 * customizing this behavior, see API Registration Options below.
 */
tracerProvider.register();

const tracer = api.trace.getTracer("foo", "1.0b");

const unwrapped = new Logger({name: "test"});
const log = wrapBunyan(unwrapped);
const span = tracer.startSpan("a span");

tracer.withSpan(span, scope => {
    log.info("test");
    const span2 = tracer.startSpan("an inner span!");
    log.child({"who": "dat"}).error("Boom!");
    span2.end();
    log.error("ohnoes!");

})

span.end();
