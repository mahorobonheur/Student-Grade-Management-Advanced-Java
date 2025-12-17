package org.example.application.service;

import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.nio.file.*;
import java.io.*;

public class AuditService {
    private final ConcurrentLinkedQueue<AuditEntry> auditLog = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOG_SIZE = 1000;
    private final ScheduledExecutorService logWriter = Executors.newSingleThreadScheduledExecutor();
    private final Path logFile = Paths.get("./logs/audit.log");

    public AuditService() {
        try {
            Files.createDirectories(logFile.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create audit log directory", e);
        }
        startLogWriter();
    }


    private static class AuditEntry {
        LocalDateTime timestamp;
        String threadId;
        String action;
        String details;
        long duration;
        boolean success;

        AuditEntry(String action, String details, long duration, boolean success) {
            this.timestamp = LocalDateTime.now();
            this.threadId = Thread.currentThread().getName();
            this.action = action;
            this.details = details;
            this.duration = duration;
            this.success = success;
        }

        @Override
        public String toString() {
            return String.format("[%s] [%s] %s - %s (Duration: %dms, Success: %s)",
                    timestamp, threadId, action, details, duration, success);
        }
    }

    public void logAction(String action) {
        logAction(action, "", 0, true);
    }

    public void logAction(String action, String details, long duration, boolean success) {
        AuditEntry entry = new AuditEntry(action, details, duration, success);
        auditLog.add(entry);

        if (auditLog.size() > MAX_LOG_SIZE) {
            auditLog.poll(); // Remove oldest entry
        }
    }

    private void startLogWriter() {
        logWriter.scheduleAtFixedRate(() -> writeLogToFile(), 0, 30, TimeUnit.SECONDS);
    }

    private void writeLogToFile() {
        try {
            List<String> lines = new ArrayList<>();
            while (!auditLog.isEmpty()) {
                AuditEntry entry = auditLog.poll();
                if (entry != null) {
                    lines.add(entry.toString());
                }
            }

            if (!lines.isEmpty()) {
                Files.write(logFile, lines,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }

    public void displayAuditTrail(Scanner scanner) {
        System.out.println("\n📋 AUDIT TRAIL VIEWER");
        System.out.println("=".repeat(80));

        System.out.println("1. View Recent Entries");
        System.out.println("2. Search by Action");
        System.out.println("3. Search by Date Range");
        System.out.println("4. View Statistics");
        System.out.println("5. Export Audit Log");
        System.out.println("6. Return to Main Menu");
        System.out.print("Select option: ");

        int choice = Integer.parseInt(scanner.nextLine().trim());

        switch (choice) {
            case 1:
                displayRecentEntries(50);
                break;
            case 2:
                searchByAction(scanner);
                break;
            case 3:
                searchByDate(scanner);
                break;
            case 4:
                displayStatistics();
                break;
            case 5:
                exportAuditLog();
                break;
        }
    }

    private void displayRecentEntries(int count) {
        System.out.println("\n📜 RECENT AUDIT ENTRIES");
        System.out.println("=".repeat(100));
        System.out.printf("%-25s | %-15s | %-20s | %-30s | %-8s%n",
                "Timestamp", "Thread", "Action", "Details", "Duration");
        System.out.println("-".repeat(100));

        auditLog.stream()
                .limit(count)
                .forEach(entry -> System.out.printf("%-25s | %-15s | %-20s | %-30s | %-8dms%n",
                        entry.timestamp,
                        entry.threadId.length() > 15 ? entry.threadId.substring(0, 12) + "..." : entry.threadId,
                        entry.action.length() > 20 ? entry.action.substring(0, 17) + "..." : entry.action,
                        entry.details.length() > 30 ? entry.details.substring(0, 27) + "..." : entry.details,
                        entry.duration));
    }

    private void searchByAction(Scanner scanner) {
        System.out.print("Enter action to search for: ");
        String searchTerm = scanner.nextLine().trim().toLowerCase();

        System.out.println("\n🔍 SEARCH RESULTS");
        System.out.println("=".repeat(100));

        auditLog.stream()
                .filter(entry -> entry.action.toLowerCase().contains(searchTerm))
                .forEach(System.out::println);
    }

    private void searchByDate(Scanner scanner) {
        System.out.print("Enter start date (YYYY-MM-DD): ");
        String startDate = scanner.nextLine().trim();
        System.out.print("Enter end date (YYYY-MM-DD): ");
        String endDate = scanner.nextLine().trim();

        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");

        System.out.println("\n📅 DATE RANGE SEARCH");
        System.out.println("=".repeat(100));

        auditLog.stream()
                .filter(entry -> !entry.timestamp.isBefore(start) && !entry.timestamp.isAfter(end))
                .forEach(System.out::println);
    }

    private void displayStatistics() {
        System.out.println("\n📊 AUDIT STATISTICS");
        System.out.println("=".repeat(40));

        long total = auditLog.size();
        long successful = auditLog.stream().filter(entry -> entry.success).count();
        long failed = total - successful;

        System.out.println("Total Entries: " + total);
        System.out.println("Successful: " + successful);
        System.out.println("Failed: " + failed);
        System.out.printf("Success Rate: %.1f%%%n",
                total == 0 ? 0 : (successful * 100.0 / total));

        // Group by action
        Map<String, Long> actionCounts = new HashMap<>();
        auditLog.forEach(entry ->
                actionCounts.merge(entry.action, 1L, Long::sum));

        System.out.println("\n📈 ACTION DISTRIBUTION");
        actionCounts.forEach((action, count) ->
                System.out.printf("%-20s: %d (%.1f%%)%n",
                        action, count, count * 100.0 / total));
    }

    private void exportAuditLog() {
        try {
            Path exportPath = Paths.get("./exports/audit_" +
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");

            List<String> lines = new ArrayList<>();
            lines.add("Timestamp,Thread,Action,Details,Duration,Success");

            auditLog.forEach(entry ->
                    lines.add(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d,%s",
                            entry.timestamp, entry.threadId, entry.action,
                            entry.details, entry.duration, entry.success)));

            Files.write(exportPath, lines);
            System.out.println("✅ Audit log exported to: " + exportPath);
        } catch (IOException e) {
            System.out.println("❌ Export failed: " + e.getMessage());
        }
    }

    public void displayGradeHistory() {
        System.out.println("\n📜 GRADE HISTORY");
        System.out.println("=".repeat(80));

        // Filter grade-related actions
        auditLog.stream()
                .filter(entry -> entry.action.contains("Grade") || entry.action.contains("GPA"))
                .limit(20)
                .forEach(System.out::println);
    }

    public void shutdown() {
        logWriter.shutdown();
        try {
            if (!logWriter.awaitTermination(5, TimeUnit.SECONDS)) {
                logWriter.shutdownNow();
            }
        } catch (InterruptedException e) {
            logWriter.shutdownNow();
        }
        writeLogToFile(); // Final write
    }
}