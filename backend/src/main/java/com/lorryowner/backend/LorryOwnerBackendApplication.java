package com.lorryowner.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.PrintStream;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point for the Lorry Owner Backend Application.
 */
@SpringBootApplication
public class LorryOwnerBackendApplication {

    private static final int WATCHDOG_DELAY_SECONDS = 30;

    public static void main(String[] args) {
        // Start daemon watchdog before SpringApplication.run()
        Thread watchdog = startStartupWatchdog(WATCHDOG_DELAY_SECONDS, System.err);

        SpringApplication.run(LorryOwnerBackendApplication.class, args);

        // Cancel the watchdog when Spring Boot startup completes successfully
        if (watchdog != null) {
            watchdog.interrupt();
        }
    }

    /**
     * Starts a standalone daemon thread that captures and prints all JVM threads
     * if application startup does not complete within the specified delay.
     */
    public static Thread startStartupWatchdog(int delaySeconds, PrintStream out) {
        Thread watchdog = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(delaySeconds);
                dumpAllThreads(out);
            } catch (InterruptedException e) {
                // Watchdog cancelled because startup completed successfully before delay
            }
        }, "startup-watchdog");

        watchdog.setDaemon(true);
        watchdog.start();
        return watchdog;
    }

    /**
     * Captures all JVM threads and prints complete stack traces directly to the specified stream.
     */
    public static void dumpAllThreads(PrintStream out) {
        out.println();
        out.println("================================================================================");
        out.printf("STARTUP WATCHDOG: FULL JVM THREAD DUMP (Captured at %s)%n", Instant.now());
        out.println("================================================================================");

        Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allThreads.entrySet()) {
            Thread t = entry.getKey();
            StackTraceElement[] stack = entry.getValue();

            out.printf("\"%s\" #%d daemon=%s prio=%d state=%s%n",
                    t.getName(), t.getId(), t.isDaemon(), t.getPriority(), t.getState());

            if (stack != null && stack.length > 0) {
                for (StackTraceElement ste : stack) {
                    out.printf("    at %s%n", ste);
                }
            } else {
                out.println("    (no stack trace available)");
            }
            out.println();
        }

        out.println("================================================================================");
        out.println("END OF STARTUP WATCHDOG THREAD DUMP");
        out.println("================================================================================");
        out.flush();
    }
}
