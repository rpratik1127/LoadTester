package org.tester.report;

import org.tester.selector.PersonaLoadConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CsvReport {

    private static final String FILE_NAME = "csvreport.csv";

    private static final String HEADER =
            "timestamp,totalUsers,durationSec,totalRequests,success,failures,errorRate,throughput," +
                    "maxRequestsSentPerSec,avgRequestsSentPerSec," +
                    "avgResponseTimeMs,minResponseTimeMs,maxResponseTimeMs,p90Ms,p95Ms,p99Ms,p999Ms," +
                    "status0,status400,status401,status403,status404,status429,status500,status502,status503,status504,otherFailures\n";

    public static synchronized void appendSummary(
            int durationSeconds,
            long totalRequests,
            long success,
            long failures,
            double errorRate,
            double throughput,
            double maxRequestsSentPerSec,
            double avgRequestsSentPerSec,
            double avgResponseTime,
            long minResponseTime,
            long maxResponseTime,
            long p90,
            long p95,
            long p99,
            long p999,
            long status0,
            long status400,
            long status401,
            long status403,
            long status404,
            long status429,
            long status500,
            long status502,
            long status503,
            long status504,
            long otherFailures
    ) {
        try {
            File file = new File(FILE_NAME);
            boolean writeHeader = !file.exists() || file.length() == 0;

            try (FileWriter writer = new FileWriter(file, true)) {
                if (writeHeader) {
                    writer.write(HEADER);
                }

                String timestamp = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                int totalUsers = PersonaLoadConfig.getTotalUsers();

                writer.write(timestamp + ",");
                writer.write(totalUsers + ",");
                writer.write(durationSeconds + ",");
                writer.write(totalRequests + ",");
                writer.write(success + ",");
                writer.write(failures + ",");
                writer.write(String.format("%.2f", errorRate) + ",");
                writer.write(String.format("%.2f", throughput) + ",");

                writer.write(String.format("%.2f", maxRequestsSentPerSec) + ",");
                writer.write(String.format("%.2f", avgRequestsSentPerSec) + ",");

                writer.write(String.format("%.2f", avgResponseTime) + ",");
                writer.write(minResponseTime + ",");
                writer.write(maxResponseTime + ",");
                writer.write(p90 + ",");
                writer.write(p95 + ",");
                writer.write(p99 + ",");
                writer.write(p999 + ",");

                writer.write(status0 + ",");
                writer.write(status400 + ",");
                writer.write(status401 + ",");
                writer.write(status403 + ",");
                writer.write(status404 + ",");
                writer.write(status429 + ",");
                writer.write(status500 + ",");
                writer.write(status502 + ",");
                writer.write(status503 + ",");
                writer.write(status504 + ",");
                writer.write(String.valueOf(otherFailures));
                writer.write("\n");
            }

            System.out.println("CSV report appended to: " + FILE_NAME);

        } catch (IOException e) {
            System.err.println("Failed to write CSV report: " + e.getMessage());
        }
    }
}
