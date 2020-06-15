package wrapper

import (
	"context"
	"go.opentelemetry.io/otel/api/trace"
	"go.uber.org/zap"
)

type ZapWrap struct {
	*zap.Logger
	context *context.Context
}

func Wrap(log *zap.Logger) ZapWrap {
	return ZapWrap {
		log,
		nil,
	}
}

func WrapWithContext(ctx context.Context, logger *zap.Logger) ZapWrap {
	return ZapWrap {
		logger,
		&ctx,
	}
}

func (log *ZapWrap) CInfo(ctx context.Context, msg string, fields ...zap.Field) {
	span := trace.SpanFromContext(ctx)
	allFields := []zap.Field{}
	allFields = append(allFields, fields...)
	if span.IsRecording() {
		context := span.SpanContext();
		spanField := zap.String("spanid", context.SpanID.String())
		traceField := zap.String("traceid", context.TraceID.String())
		traceFlags:= zap.Int("traceflags", int(context.TraceFlags))
		allFields = append(allFields, []zap.Field{spanField, traceField, traceFlags}...)
	}
	log.Logger.Info(msg, allFields...)
}

func (log *ZapWrap) Info(msg string, fields ...zap.Field) {
	if log.context != nil {
		log.CInfo(*log.context, msg, fields...)
	} else {
		log.Logger.Info(msg, fields...)
	}
}