package org.tester;

import org.tester.control.ThroughputController;
import org.tester.metrics.MetricsCollector;
import org.tester.model.TestPlan;
import org.tester.parser.PersonaParser;
import org.tester.report.CsvReportGenerator;
import org.tester.report.ReportGenerator;
import org.tester.runner.ConcurrentPersonaRunner;
import org.tester.selector.PersonaUserSelector;
import org.tester.selector.RampUpSelector;
import org.tester.selector.TargetTpsSelector;
import org.tester.selector.TestDurationSelector;

import org.tester.selector.PersonaLoadConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {

        PersonaParser personaParser = new PersonaParser();
        TestPlan testPlan = personaParser.parse("personas/persona.json");

        PersonaUserSelector userSelector = new PersonaUserSelector();
        PersonaLoadConfig loadConfig = userSelector.selectLoadConfig(testPlan.personas);

        TestDurationSelector durationSelector = new TestDurationSelector();
        int durationSeconds = durationSelector.selectDurationInSeconds();

        RampUpSelector rampUpSelector = new RampUpSelector();
        int rampUpSeconds = rampUpSelector.selectRampUpSeconds(durationSeconds);

        TargetTpsSelector targetTpsSelector = new TargetTpsSelector();
        int targetTps = targetTpsSelector.selectTargetTps();

        ThroughputController throughputController =
                targetTps > 0 ? new ThroughputController(targetTps) : null;

        MetricsCollector metricsCollector = new MetricsCollector();
        ConcurrentPersonaRunner runner = new ConcurrentPersonaRunner();

        // ✅ Live TPS reporter
        ScheduledExecutorService liveReporter = Executors.newSingleThreadScheduledExecutor();
        liveReporter.scheduleAtFixedRate(() ->
                        System.out.printf("[LIVE] TPS: %.0f | Total: %d | Errors: %.1f%%%n",
                                metricsCollector.getCurrentTps(),
                                metricsCollector.getTotalRequests(),
                                metricsCollector.getErrorRate()
                        ),
                1, 1, TimeUnit.SECONDS
        );

        // ✅ Track actual elapsed time
        long startTime = System.currentTimeMillis();

        try {
            runner.runPersonas(
                    testPlan.personas,
                    loadConfig,
                    durationSeconds,
                    rampUpSeconds,
                    metricsCollector,
                    throughputController
            );
        } finally {
            liveReporter.shutdownNow();
            if (throughputController != null) throughputController.shutdown();
        }

        // ✅ Always runs, uses actual elapsed time
        int actualDuration = (int) ((System.currentTimeMillis() - startTime) / 1000);

        ReportGenerator reportGenerator = new ReportGenerator();
        reportGenerator.printSummary(metricsCollector, actualDuration, testPlan.personas);

        CsvReportGenerator csvReportGenerator = new CsvReportGenerator();
        csvReportGenerator.generateStepReport(
                metricsCollector, testPlan.personas, "step-report.csv");
        csvReportGenerator.generateDetailedRequestReport(
                metricsCollector, "request-log.csv");
    }
}