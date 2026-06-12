package org.tester.runner;

import org.tester.control.PersonaRequestLimiter;
import org.tester.control.ThroughputController;
import org.tester.executor.StepExecutor;
import org.tester.metrics.MetricsCollector;
import org.tester.model.Persona;
import org.tester.selector.LoadInputMode;
import org.tester.selector.PersonaLoadConfig;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrentPersonaRunner {

    public void runPersonas(
            List<Persona> personas,
            PersonaLoadConfig loadConfig,
            int durationSeconds,
            int rampUpSeconds,
            MetricsCollector metricsCollector,
            ThroughputController throughputController
    ) throws InterruptedException {

        StepExecutor stepExecutor = new StepExecutor(metricsCollector);

        int totalUsers = PersonaLoadConfig.getTotalUsers();

        if (totalUsers == 0) {
            System.out.println("No load configured. Load test will not run.");
            return;
        }

        PersonaRequestLimiter requestLimiter = null;
        if (loadConfig.mode == LoadInputMode.REQUESTS) {
            requestLimiter = new PersonaRequestLimiter(loadConfig.valuesPerPersona);
        }

        long endTimeMillis = System.currentTimeMillis() + (durationSeconds * 1000L);

        long delayBetweenUsersMs = 0;
        if (rampUpSeconds > 0 && totalUsers > 1) {
            delayBetweenUsersMs = (rampUpSeconds * 1000L) / totalUsers;
        }

        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

        for (Persona persona : personas) {
            int configuredValue = loadConfig.valuesPerPersona.getOrDefault(persona.name, 0);
            if (configuredValue == 0) {
                continue;
            }

            int userCount = loadConfig.mode == LoadInputMode.USERS
                    ? configuredValue
                    : 1;

            for (int localUserId = 1; localUserId <= userCount; localUserId++) {
                String userId = persona.name + "-User-" + localUserId;

                VirtualUser virtualUser = new VirtualUser(
                        userId,
                        persona,
                        endTimeMillis,
                        metricsCollector,
                        stepExecutor,
                        throughputController,
                        requestLimiter
                );

                executorService.submit(virtualUser);

                if (delayBetweenUsersMs > 0) {
                    Thread.sleep(delayBetweenUsersMs);
                }
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(
                durationSeconds + rampUpSeconds + 60L,
                TimeUnit.SECONDS
        );
    }
}