package org.tester.executor;

import org.tester.metrics.MetricsCollector;
import org.tester.metrics.RequestMetric;
import org.tester.model.ApiStep;
import org.tester.model.Persona;
import org.tester.model.ProtocolType;
import org.tester.runtime.VariableStore;

import java.util.concurrent.CompletableFuture;

/**
 * Dispatches a single persona step to the HTTP or WebSocket executor based on protocol.
 */
public class StepExecutor {

    private final HttpExecutor httpExecutor;
    private final WebSocketExecutor webSocketExecutor;

    public StepExecutor(MetricsCollector metricsCollector) {
        this.httpExecutor = new HttpExecutor(metricsCollector);
        this.webSocketExecutor = new WebSocketExecutor();
    }

    public CompletableFuture<RequestMetric> executeAsync(
            String userId,
            Persona persona,
            ApiStep step,
            VariableStore variableStore
    ) {
        if (isHttpProtocol(step.protocol)) {
            return httpExecutor.executeAsync(
                    userId,
                    persona.name,
                    persona.baseUrl,
                    step,
                    variableStore
            );
        }

        if (step.protocol == ProtocolType.WEBSOCKET) {
            return webSocketExecutor.executeAsync(userId, persona.name, step);
        }

        return CompletableFuture.failedFuture(
                new IllegalArgumentException("Unsupported protocol: " + step.protocol)
        );
    }

    private static boolean isHttpProtocol(ProtocolType protocol) {
        return protocol == ProtocolType.HTTP
                || protocol == ProtocolType.HTTPS
                || protocol == ProtocolType.REST;
    }
}
