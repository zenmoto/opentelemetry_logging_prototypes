package main

import (
	"context"
	_ "github.com/apache/thrift/lib/go/thrift"
	"github.com/zenmoto/opentelemetry_logging_prototypes/zap/wrapper"
	otelglobal "go.opentelemetry.io/otel/api/global"
	"go.opentelemetry.io/otel/exporters/trace/jaeger"
	"go.uber.org/zap"
	_ "google.golang.org/api/support/bundler"
	"log"
)

func main() {
	flush := initTracer()
	defer flush()
	tracer := otelglobal.Tracer("opentelemetry_zap_prototype")
	zapLogger := zap.NewExample()
	logger := wrapper.Wrap(zapLogger)
	defer logger.Sync()
	tracer.WithSpan(context.Background(), "outer",
		func(ctx context.Context) error {
			logger.CInfo(ctx, "Logging with context")
			tracer.WithSpan(ctx, "inner",
				func(ctx context.Context) error {
					innerLogger := wrapper.WrapWithContext(ctx, zapLogger)
					innerLogger.Info("Logger has been wrapped with context", zap.String("key", "value"))
					return nil
				})
			return nil
		})
}

func initTracer() func() {
	tp, flush, err := jaeger.NewExportPipeline(jaeger.WithCollectorEndpoint("http://localhost:14268/api/traces"))
	if err != nil {
		log.Fatal(err)
	}
	otelglobal.SetTraceProvider(tp)
	return flush
}
