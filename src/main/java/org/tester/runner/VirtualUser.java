package org.tester.runner;

import org.tester.control.PersonaRequestLimiter;
import org.tester.control.ThroughputController;
import org.tester.executor.StepExecutor;
import org.tester.metrics.MetricsCollector;
import org.tester.metrics.RequestMetric;
import org.tester.model.ApiStep;
import org.tester.model.Persona;
import org.tester.runtime.VariableStore;

public class VirtualUser implements Runnable {

    private final String userId;
    private final Persona persona;
    private final long endTimeMillis;
    private final MetricsCollector metricsCollector;
    private final StepExecutor stepExecutor;
    private final ThroughputController throughputController;
    private final PersonaRequestLimiter requestLimiter;

    public VirtualUser(
            String userId,
            Persona persona,
            long endTimeMillis,
            MetricsCollector metricsCollector,
            StepExecutor stepExecutor,
            ThroughputController throughputController,
            PersonaRequestLimiter requestLimiter
    ) {
        this.userId = userId;
        this.persona = persona;
        this.endTimeMillis = endTimeMillis;
        this.metricsCollector = metricsCollector;
        this.stepExecutor = stepExecutor;
        this.throughputController = throughputController;
        this.requestLimiter = requestLimiter;
    }

    @Override
    public void run() {
        VariableStore variableStore = new VariableStore();

        // ✅ Fix 5 — removed println from hot path entirely
        // only log errors, and even those should go to a concurrent structure

        while (System.currentTimeMillis() < endTimeMillis) {
            for (ApiStep step : persona.steps) {
                if (System.currentTimeMillis() >= endTimeMillis) break;

                if (requestLimiter != null && !requestLimiter.tryAcquire(persona.name)) {
                    return;
                }

                try {
                    if (throughputController != null) {
                        throughputController.acquire();
                    }

                    RequestMetric metric = stepExecutor.execute(userId, persona, step, variableStore);
                    metricsCollector.add(metric);

                    if (step.thinkTimeMs != null && step.thinkTimeMs > 0) {
                        Thread.sleep(step.thinkTimeMs);
                    }

                } catch (Exception e) {

                    // ✅ Don't println here — record as a failed metric instead
                    metricsCollector.add(RequestMetric.failed(userId, persona.name, step.name, e.getMessage()));
                }
            }
        }
    }
}