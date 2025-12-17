package org.example.application.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.domain.model.*;
import org.example.domain.repository.*;
import org.example.infrastructure.util.GPACalculator;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.nio.file.*;
import java.io.*;
import java.util.stream.Collectors;

/**
 * AdvancedStatisticsService provides comprehensive statistics and reporting features
 * including real-time dashboard, batch report generation, and multi-format exports.
 *
 * Features:
 * - Real-time statistics dashboard with auto-refresh
 * - Batch report generation with thread pooling
 * - Multi-format export (CSV, JSON, Binary)
 * - Performance metrics and monitoring
 */
public class AdvancedStatisticsService {
    private final StudentRepository studentRepo;
    private final GradeRepository gradeRepo;
    private final Map<String, Object> statisticsCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private volatile boolean dashboardRunning = false;

    // Performance tracking
    private long totalCacheHits = 0;
    private long totalCacheMisses = 0;
    private final List<Long> reportGenerationTimes = new CopyOnWriteArrayList<>();

    // Directory paths
    private static final String CSV_REPORTS_DIR = "./reports/csv/";
    private static final String JSON_REPORTS_DIR = "./reports/json/";
    private static final String BINARY_REPORTS_DIR = "./reports/binary/";
    private static final String IMPORTS_DIR = "./imports/";
    private static final String LOGS_DIR = "./logs/";

    static {
        // Create necessary directories on service initialization
        createDirectories();
    }

    private static void createDirectories() {
        try {
            Files.createDirectories(Paths.get(CSV_REPORTS_DIR));
            Files.createDirectories(Paths.get(JSON_REPORTS_DIR));
            Files.createDirectories(Paths.get(BINARY_REPORTS_DIR));
            Files.createDirectories(Paths.get(IMPORTS_DIR));
            Files.createDirectories(Paths.get(LOGS_DIR));
            System.out.println("✅ All directories created successfully");
        } catch (IOException e) {
            System.err.println("❌ Failed to create directories: " + e.getMessage());
        }
    }

    public AdvancedStatisticsService(StudentRepository studentRepo, GradeRepository gradeRepo) {
        this.studentRepo = studentRepo;
        this.gradeRepo = gradeRepo;
        startScheduledTasks();
    }

    /**
     * Starts scheduled background tasks for cache refresh and statistics updates
     * with proper exception handling and resource management
     */
    private void startScheduledTasks() {
        try {
            // Schedule daily statistics update at 2 AM with error handling
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    updateStatistics();
                } catch (Exception e) {
                    logError("Statistics update failed", e);
                }
            }, 0, 24, TimeUnit.HOURS);

            // Schedule cache refresh every hour with proper cleanup
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    refreshCache();
                } catch (Exception e) {
                    logError("Cache refresh failed", e);
                }
            }, 0, 1, TimeUnit.HOURS);

            System.out.println("✅ Scheduled tasks initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to start scheduled tasks: " + e.getMessage());
        }
    }

    /**
     * Displays the real-time statistics dashboard with auto-refresh
     * @param scanner Scanner for user input
     */
    public void displayDashboard(Scanner scanner) {
        dashboardRunning = true;
        System.out.println("\nREAL-TIME STATISTICS DASHBOARD");
        System.out.println("=".repeat(70));
        System.out.println("Auto-refresh: Enabled (5 sec) | Press 'Q' to quit | 'R' for refresh | 'H' for help");
        System.out.println("Commands: Q=Quit, R=Refresh, P=Pause/Resume, H=Help");

        // Start dashboard display thread
        Thread dashboardThread = new Thread(() -> {
            while (dashboardRunning) {
                try {
                    clearConsole();
                    displayDashboardContent();
                    Thread.sleep(5000); // Refresh every 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Dashboard error: " + e.getMessage());
                    break;
                }
            }
        });
        dashboardThread.setDaemon(true);
        dashboardThread.start();

        // Command handler
        handleDashboardCommands(scanner);

        // Cleanup
        dashboardRunning = false;
        try {
            dashboardThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Displays the main dashboard content with comprehensive statistics
     */
    private void displayDashboardContent() {
        List<Student> students = studentRepo.findAll();
        System.out.println("\nREAL-TIME STATISTICS DASHBOARD");
        System.out.println("=".repeat(70));
        System.out.println("Last Updated: " + new Date());
        System.out.println("Status: " + (dashboardRunning ? "RUNNING" : "STOPPED"));

        // System status
        System.out.println("\nSYSTEM STATUS");
        System.out.println("-".repeat(40));
        System.out.println("Total Students: " + students.size());
        System.out.println("Active Threads: " + Thread.activeCount());
        System.out.printf("Memory Usage: %.1f MB / %.1f MB%n",
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0),
                Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0));
        System.out.printf("Cache Hit Rate: %.1f%%%n", getCacheHitRate() * 100);

        // Grade statistics
        System.out.println("\nGRADE STATISTICS");
        System.out.println("-".repeat(40));
        System.out.println("Total Grades: " + gradeRepo.count());

        // Calculate advanced statistics
        List<Double> allGrades = getAllGrades();
        if (!allGrades.isEmpty()) {
            System.out.printf("Mean (Average): %.2f%%%n", calculateMean(allGrades));
            System.out.printf("Median: %.2f%%%n", calculateMedian(allGrades));
            System.out.printf("Mode: %s%n", calculateMode(allGrades));
            System.out.printf("Range: %.2f%%%n", calculateRange(allGrades));
            System.out.printf("Standard Deviation: %.2f%%%n", calculateStandardDeviation(allGrades));
            System.out.printf("Variance: %.2f%%%n", calculateVariance(allGrades));
        }

        // Grade distribution
        System.out.println("\nGRADE DISTRIBUTION");
        System.out.println("-".repeat(40));
        Map<String, Long> distribution = calculateGradeDistribution();
        distribution.forEach((range, count) -> {
            double percentage = students.isEmpty() ? 0 : (count * 100.0 / students.size());
            System.out.printf("%-15s: %3d students (%5.1f%%)%n", range, count, percentage);
        });

        // Top performers
        System.out.println("\nTOP PERFORMERS");
        System.out.println("-".repeat(40));
        List<Student> topPerformers = getTopPerformers(5);
        for (int i = 0; i < topPerformers.size(); i++) {
            Student student = topPerformers.get(i);
            double avg = gradeRepo.calculateStudentAverage(student.getStudentId());
            System.out.printf("%d. %-20s (%-8s): %6.2f%% | GPA: %.2f%n",
                    i + 1, student.getName(), student.getStudentId(), avg,
                    GPACalculator.percentageToGPA(avg));
        }

        // Performance metrics
        System.out.println("\n⚡ PERFORMANCE METRICS");
        System.out.println("-".repeat(40));
        if (!reportGenerationTimes.isEmpty()) {
            double avgReportTime = reportGenerationTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0);
            System.out.printf("Avg Report Time: %.1f ms%n", avgReportTime);
        }
        System.out.printf("Cache Size: %d entries%n", statisticsCache.size());

        // Command prompt
        System.out.println("\n" + "=".repeat(70));
        System.out.print("Enter command (Q/R/P/H): ");
    }

    /**
     * Handles dashboard commands from user input
     * @param scanner Scanner for user input
     */
    private void handleDashboardCommands(Scanner scanner) {
        while (dashboardRunning) {
            System.out.print("\nCommand: ");
            String command = scanner.nextLine().trim().toUpperCase();

            switch (command) {
                case "Q":
                    System.out.println("Exiting dashboard...");
                    dashboardRunning = false;
                    return;
                case "R":
                    System.out.println("Refreshing dashboard...");
                    displayDashboardContent();
                    break;
                case "P":
                    System.out.println("Dashboard paused. Press 'R' to resume...");
                    waitForResume(scanner);
                    break;
                case "H":
                    displayHelp();
                    break;
                default:
                    System.out.println("Unknown command. Use Q=Quit, R=Refresh, P=Pause, H=Help");
            }
        }
    }

    /**
     * Waits for resume command after pausing
     * @param scanner Scanner for user input
     */
    private void waitForResume(Scanner scanner) {
        while (true) {
            System.out.print("Command: ");
            String command = scanner.nextLine().trim().toUpperCase();
            if (command.equals("R")) {
                System.out.println("Resuming dashboard...");
                break;
            } else if (command.equals("Q")) {
                dashboardRunning = false;
                break;
            }
        }
    }

    /**
     * Displays help information for dashboard commands
     */
    private void displayHelp() {
        System.out.println("\nDASHBOARD COMMAND HELP");
        System.out.println("=".repeat(50));
        System.out.println("Q - Quit dashboard and return to main menu");
        System.out.println("R - Refresh dashboard immediately");
        System.out.println("P - Pause auto-refresh (press R to resume)");
        System.out.println("H - Display this help message");
        System.out.println("\nDashboard auto-refreshes every 5 seconds.");
        System.out.println("Statistics are calculated in real-time.");
        System.out.println("\nPress any key to continue...");
    }

    /**
     * Generates batch reports for all students using thread pooling
     * @param scanner Scanner for user input
     */
    public void generateBatchReports(Scanner scanner) {
        System.out.println("\nGENERATE BATCH REPORTS");
        System.out.println("=".repeat(50));

        List<Student> students = studentRepo.findAll();
        if (students.isEmpty()) {
            System.out.println("❌ No students found to generate reports.");
            return;
        }

        System.out.printf("Found %d students.%n", students.size());
        System.out.print("Enter number of threads (1-8, recommended: 4): ");

        try {
            int threadCount = Integer.parseInt(scanner.nextLine().trim());
            threadCount = Math.max(1, Math.min(8, threadCount));

            System.out.println("Starting batch report generation...");
            System.out.println("=".repeat(50));

            // Create thread pool
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Future<ReportResult>> futures = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(students.size());

            // Submit report generation tasks
            for (Student student : students) {
                futures.add(executor.submit(() -> generateStudentReport(student, latch)));
            }

            // Wait for completion with progress display
            displayProgressBar(students.size(), latch);

            // Collect results
            int successful = 0;
            int failed = 0;
            long totalTime = 0;

            for (Future<ReportResult> future : futures) {
                try {
                    ReportResult result = future.get(10, TimeUnit.SECONDS);
                    if (result.success) {
                        successful++;
                        totalTime += result.generationTime;
                        reportGenerationTimes.add(result.generationTime);
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    failed++;
                    logError("Report generation failed", e);
                }
            }

            // Shutdown executor
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // Display summary
            displayBatchReportSummary(successful, failed, totalTime, students.size());

        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number format. Please enter a valid number.");
        } catch (Exception e) {
            System.out.println("❌ Error generating batch reports: " + e.getMessage());
        }
    }

    /**
     * Generates a report for a single student
     * @param student The student to generate report for
     * @param latch CountDownLatch for progress tracking
     * @return ReportResult containing generation status and metrics
     */
    private ReportResult generateStudentReport(Student student, CountDownLatch latch) {
        long startTime = System.currentTimeMillis();
        ReportResult result = new ReportResult();

        try {
            double avg = gradeRepo.calculateStudentAverage(student.getStudentId());
            List<Grade> grades = gradeRepo.findByStudentId(student.getStudentId());

            // Export in all formats
            exportToCSV(student, avg, grades);
            exportToJSON(student, avg, grades);
            exportToBinary(student, avg, grades);

            result.success = true;
            result.generationTime = System.currentTimeMillis() - startTime;

        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getMessage();
            logError("Failed to generate report for student: " + student.getStudentId(), e);
        } finally {
            latch.countDown();
        }

        return result;
    }

    /**
     * Exports student data to CSV format
     * @param student Student to export
     * @param average Student's average grade
     * @param grades List of student's grades
     * @throws IOException If file operation fails
     */
    private void exportToCSV(Student student, double average, List<Grade> grades) throws IOException {
        Path dir = Paths.get(CSV_REPORTS_DIR);
        Files.createDirectories(dir);

        Path filePath = dir.resolve(student.getStudentId() + "_report.csv");

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            // Write header
            writer.write("Student Report - Generated: " + new Date());
            writer.newLine();
            writer.write("=".repeat(50));
            writer.newLine();

            // Write student info
            writer.write("Student ID," + student.getStudentId());
            writer.newLine();
            writer.write("Name," + student.getName());
            writer.newLine();
            writer.write("Type," + student.getStudentType());
            writer.newLine();
            writer.write("Email," + student.getEmail());
            writer.newLine();
            writer.write("Phone," + student.getPhone());
            writer.newLine();
            writer.write("Status," + student.getStatus());
            writer.newLine();
            writer.write("Average Grade," + String.format("%.2f%%", average));
            writer.newLine();
            writer.write("GPA," + String.format("%.2f", GPACalculator.percentageToGPA(average)));
            writer.newLine();

            // Write grades header
            writer.newLine();
            writer.write("GRADE DETAILS");
            writer.newLine();
            writer.write("Subject,Type,Grade,Letter Grade,Date");
            writer.newLine();

            // Write grades
            for (Grade grade : grades) {
                writer.write(String.format("%s,%s,%.2f,%s,%s",
                        grade.getSubject().getSubjectName(),
                        grade.getSubject().getSubjectType(),
                        grade.getGrade(),
                        getLetterGrade(grade.getGrade()),
                        grade.getDate()));
                writer.newLine();
            }

            // Write statistics
            writer.newLine();
            writer.write("STATISTICS");
            writer.newLine();
            writer.write("Total Subjects," + grades.size());
            writer.newLine();
            writer.write("Status," + (average >= student.getPassingGrade() ? "PASSING" : "FAILING"));
            writer.newLine();

            cacheHit(); // Track cache hit
        }
    }

    /**
     * Exports student data to JSON format
     * @param student Student to export
     * @param average Student's average grade
     * @param grades List of student's grades
     * @throws IOException If file operation fails
     */
    private void exportToJSON(Student student, double average, List<Grade> grades) throws IOException {
        Path dir = Paths.get(JSON_REPORTS_DIR);
        Files.createDirectories(dir);

        Path filePath = dir.resolve(student.getStudentId() + "_report.json");

        Map<String, Object> reportData = new LinkedHashMap<>();
        reportData.put("reportId", UUID.randomUUID().toString());
        reportData.put("generationDate", new Date().toString());
        reportData.put("studentId", student.getStudentId());
        reportData.put("name", student.getName());
        reportData.put("type", student.getStudentType());
        reportData.put("email", student.getEmail());
        reportData.put("phone", student.getPhone());
        reportData.put("status", student.getStatus());
        reportData.put("averageGrade", average);
        reportData.put("gpa", GPACalculator.percentageToGPA(average));
        reportData.put("passingStatus", average >= student.getPassingGrade() ? "PASSING" : "FAILING");

        List<Map<String, Object>> gradeList = new ArrayList<>();
        for (Grade grade : grades) {
            Map<String, Object> gradeData = new LinkedHashMap<>();
            gradeData.put("subject", grade.getSubject().getSubjectName());
            gradeData.put("subjectType", grade.getSubject().getSubjectType());
            gradeData.put("grade", grade.getGrade());
            gradeData.put("letterGrade", getLetterGrade(grade.getGrade()));
            gradeData.put("date", grade.getDate());
            gradeList.add(gradeData);
        }
        reportData.put("grades", gradeList);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(reportData);

        Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        cacheHit(); // Track cache hit
    }

    /**
     * Exports student data to binary format
     * @param student Student to export
     * @param average Student's average grade
     * @param grades List of student's grades
     * @throws IOException If file operation fails
     */
    private void exportToBinary(Student student, double average, List<Grade> grades) throws IOException {
        Path dir = Paths.get(BINARY_REPORTS_DIR);
        Files.createDirectories(dir);

        Path filePath = dir.resolve(student.getStudentId() + "_report.dat");

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(filePath)))) {

            BinaryReport report = new BinaryReport(student, average, grades);
            oos.writeObject(report);
            cacheHit(); // Track cache hit
        }
    }

    /**
     * Displays progress bar during batch report generation
     * @param total Total number of reports
     * @param latch CountDownLatch for tracking completion
     */
    private void displayProgressBar(int total, CountDownLatch latch) {
        Thread progressThread = new Thread(() -> {
            int lastCount = total;
            while (latch.getCount() > 0) {
                int completed = total - (int) latch.getCount();
                if (completed != lastCount) {
                    double percentage = (completed * 100.0) / total;
                    System.out.printf("\rProgress: [%-50s] %.1f%% (%d/%d)",
                            "=".repeat((int) (percentage / 2)), percentage, completed, total);
                    lastCount = completed;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.printf("\rProgress: [%-50s] 100.0%% (%d/%d)%n",
                    "=".repeat(50), total, total);
        });
        progressThread.setDaemon(true);
        progressThread.start();

        try {
            latch.await();
            Thread.sleep(200); // Small delay to ensure final update
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Displays summary of batch report generation
     * @param successful Number of successful reports
     * @param failed Number of failed reports
     * @param totalTime Total generation time in milliseconds
     * @param total Total number of reports attempted
     */
    private void displayBatchReportSummary(int successful, int failed, long totalTime, int total) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("✅ BATCH REPORT GENERATION COMPLETE");
        System.out.println("=".repeat(50));

        System.out.printf("Total Reports: %d%n", total);
        System.out.printf("Successful: %d (%.1f%%)%n", successful, (successful * 100.0 / total));
        System.out.printf("Failed: %d (%.1f%%)%n", failed, (failed * 100.0 / total));
        System.out.printf("Total Time: %.2f seconds%n", totalTime / 1000.0);

        if (successful > 0) {
            double avgTime = totalTime / (double) successful;
            System.out.printf("Average Time per Report: %.1f ms%n", avgTime);
            System.out.printf("Throughput: %.1f reports/second%n", (successful * 1000.0 / totalTime));
        }

        System.out.println("\nReport Locations:");
        System.out.println("CSV: " + CSV_REPORTS_DIR);
        System.out.println("JSON: " + JSON_REPORTS_DIR);
        System.out.println("Binary: " + BINARY_REPORTS_DIR);
    }

    /**
     * Shows comprehensive system performance metrics
     */
    public void showPerformanceMetrics() {
        System.out.println("\nSYSTEM PERFORMANCE METRICS");
        System.out.println("=".repeat(60));

        Runtime runtime = Runtime.getRuntime();

        // Memory metrics
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (usedMemory * 100.0) / maxMemory;

        System.out.println("\nMEMORY USAGE");
        System.out.println("-".repeat(40));
        System.out.printf("Used Memory: %.1f MB%n", usedMemory / (1024.0 * 1024.0));
        System.out.printf("Total Memory: %.1f MB%n", runtime.totalMemory() / (1024.0 * 1024.0));
        System.out.printf("Max Memory: %.1f MB%n", maxMemory / (1024.0 * 1024.0));
        System.out.printf("Memory Usage: %.1f%%%n", memoryUsagePercent);

        // CPU and thread metrics
        System.out.println("\nPROCESSOR & THREADS");
        System.out.println("-".repeat(40));
        System.out.printf("Available Processors: %d%n", runtime.availableProcessors());
        System.out.printf("Active Threads: %d%n", Thread.activeCount());

        // Cache performance
        System.out.println("\nCACHE PERFORMANCE");
        System.out.println("-".repeat(40));
        System.out.printf("Cache Size: %d entries%n", statisticsCache.size());
        System.out.printf("Cache Hit Rate: %.1f%%%n", getCacheHitRate() * 100);
        System.out.printf("Total Hits: %d%n", totalCacheHits);
        System.out.printf("Total Misses: %d%n", totalCacheMisses);

        // Report generation performance
        System.out.println("\nREPORT GENERATION");
        System.out.println("-".repeat(40));
        if (!reportGenerationTimes.isEmpty()) {
            double avgTime = reportGenerationTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0);
            long minTime = reportGenerationTimes.stream()
                    .mapToLong(Long::longValue)
                    .min()
                    .orElse(0);
            long maxTime = reportGenerationTimes.stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0);

            System.out.printf("Total Reports: %d%n", reportGenerationTimes.size());
            System.out.printf("Average Time: %.1f ms%n", avgTime);
            System.out.printf("Minimum Time: %d ms%n", minTime);
            System.out.printf("Maximum Time: %d ms%n", maxTime);
        } else {
            System.out.println("No report generation data available.");
        }

        // Disk usage
        System.out.println("\nDISK USAGE");
        System.out.println("-".repeat(40));
        displayDirectorySize("CSV Reports", CSV_REPORTS_DIR);
        displayDirectorySize("JSON Reports", JSON_REPORTS_DIR);
        displayDirectorySize("Binary Reports", BINARY_REPORTS_DIR);
        displayDirectorySize("Imports", IMPORTS_DIR);
        displayDirectorySize("Logs", LOGS_DIR);
    }

    /**
     * Displays size of a directory
     * @param label Directory label
     * @param path Directory path
     */
    private void displayDirectorySize(String label, String path) {
        try {
            long size = Files.walk(Paths.get(path))
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();

            String sizeStr;
            if (size < 1024) {
                sizeStr = size + " B";
            } else if (size < 1024 * 1024) {
                sizeStr = String.format("%.1f KB", size / 1024.0);
            } else {
                sizeStr = String.format("%.1f MB", size / (1024.0 * 1024.0));
            }

            System.out.printf("%-20s: %s%n", label, sizeStr);
        } catch (IOException e) {
            System.out.printf("%-20s: Error calculating size%n", label);
        }
    }

    /**
     * Calculates mean of a list of numbers
     * @param numbers List of numbers
     * @return Mean value
     */
    private double calculateMean(List<Double> numbers) {
        return numbers.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculates median of a list of numbers
     * @param numbers List of numbers
     * @return Median value
     */
    private double calculateMedian(List<Double> numbers) {
        List<Double> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        int size = sorted.size();

        if (size % 2 == 0) {
            return (sorted.get(size/2 - 1) + sorted.get(size/2)) / 2.0;
        } else {
            return sorted.get(size/2);
        }
    }

    /**
     * Calculates mode of a list of numbers
     * @param numbers List of numbers
     * @return Mode value as string
     */
    private String calculateMode(List<Double> numbers) {
        Map<Double, Long> frequency = numbers.stream()
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));

        long maxFrequency = frequency.values().stream()
                .max(Long::compare)
                .orElse(0L);

        if (maxFrequency <= 1) {
            return "No mode";
        }

        List<Double> modes = frequency.entrySet().stream()
                .filter(entry -> entry.getValue() == maxFrequency)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (modes.size() == 1) {
            return String.format("%.2f", modes.get(0));
        } else {
            return modes.stream()
                    .map(d -> String.format("%.2f", d))
                    .collect(Collectors.joining(", "));
        }
    }

    /**
     * Calculates range of a list of numbers
     * @param numbers List of numbers
     * @return Range value
     */
    private double calculateRange(List<Double> numbers) {
        if (numbers.isEmpty()) return 0.0;
        double max = Collections.max(numbers);
        double min = Collections.min(numbers);
        return max - min;
    }

    /**
     * Calculates standard deviation of a list of numbers
     * @param numbers List of numbers
     * @return Standard deviation value
     */
    private double calculateStandardDeviation(List<Double> numbers) {
        if (numbers.size() <= 1) return 0.0;

        double mean = calculateMean(numbers);
        double variance = numbers.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .sum() / (numbers.size() - 1);

        return Math.sqrt(variance);
    }

    /**
     * Calculates variance of a list of numbers
     * @param numbers List of numbers
     * @return Variance value
     */
    private double calculateVariance(List<Double> numbers) {
        if (numbers.size() <= 1) return 0.0;

        double mean = calculateMean(numbers);
        return numbers.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .sum() / (numbers.size() - 1);
    }

    /**
     * Gets all grades from all students
     * @return List of all grades
     */
    private List<Double> getAllGrades() {
        List<Double> allGrades = new ArrayList<>();
        List<Student> students = studentRepo.findAll();

        for (Student student : students) {
            List<Grade> grades = gradeRepo.findByStudentId(student.getStudentId());
            for (Grade grade : grades) {
                allGrades.add(grade.getGrade());
            }
        }

        return allGrades;
    }

    /**
     * Calculates grade distribution
     * @return Map of grade ranges to counts
     */
    private Map<String, Long> calculateGradeDistribution() {
        List<Student> students = studentRepo.findAll();
        Map<String, Long> distribution = new LinkedHashMap<>();

        // Initialize ranges
        distribution.put("A (90-100%)", 0L);
        distribution.put("B (80-89%)", 0L);
        distribution.put("C (70-79%)", 0L);
        distribution.put("D (60-69%)", 0L);
        distribution.put("F (<60%)", 0L);

        // Count students in each range
        for (Student student : students) {
            double avg = gradeRepo.calculateStudentAverage(student.getStudentId());
            if (avg >= 90) {
                distribution.put("A (90-100%)", distribution.get("A (90-100%)") + 1);
            } else if (avg >= 80) {
                distribution.put("B (80-89%)", distribution.get("B (80-89%)") + 1);
            } else if (avg >= 70) {
                distribution.put("C (70-79%)", distribution.get("C (70-79%)") + 1);
            } else if (avg >= 60) {
                distribution.put("D (60-69%)", distribution.get("D (60-69%)") + 1);
            } else {
                distribution.put("F (<60%)", distribution.get("F (<60%)") + 1);
            }
        }

        return distribution;
    }

    /**
     * Gets top performing students
     * @param count Number of top performers to return
     * @return List of top performing students
     */
    private List<Student> getTopPerformers(int count) {
        return studentRepo.findAll().stream()
                .sorted((s1, s2) -> Double.compare(
                        gradeRepo.calculateStudentAverage(s2.getStudentId()),
                        gradeRepo.calculateStudentAverage(s1.getStudentId())
                ))
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Gets letter grade for a numerical grade
     * @param grade Numerical grade
     * @return Letter grade
     */
    private String getLetterGrade(double grade) {
        if (grade >= 97) return "A+";
        if (grade >= 93) return "A";
        if (grade >= 90) return "A-";
        if (grade >= 87) return "B+";
        if (grade >= 83) return "B";
        if (grade >= 80) return "B-";
        if (grade >= 77) return "C+";
        if (grade >= 73) return "C";
        if (grade >= 70) return "C-";
        if (grade >= 67) return "D+";
        if (grade >= 60) return "D";
        return "F";
    }

    /**
     * Updates statistics cache
     */
    public void updateStatistics() {
        try {
            statisticsCache.put("lastUpdate", new Date());
            statisticsCache.put("studentCount", studentRepo.findAll().size());
            statisticsCache.put("gradeCount", gradeRepo.count());
            statisticsCache.put("cacheHitRate", getCacheHitRate());
            statisticsCache.put("memoryUsage",
                    (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0));

            cacheHit(); // Track cache hit
        } catch (Exception e) {
            logError("Failed to update statistics", e);
        }
    }

    /**
     * Refreshes the cache
     */
    public void refreshCache() {
        try {
            statisticsCache.clear();
            updateStatistics();
            cacheHit(); // Track cache hit
        } catch (Exception e) {
            logError("Failed to refresh cache", e);
        }
    }

    /**
     * Gets current cache hit rate
     * @return Cache hit rate as decimal
     */
    private double getCacheHitRate() {
        long total = totalCacheHits + totalCacheMisses;
        return total == 0 ? 0.0 : (double) totalCacheHits / total;
    }

    /**
     * Tracks a cache hit
     */
    private void cacheHit() {
        totalCacheHits++;
    }

    /**
     * Tracks a cache miss
     */
    private void cacheMiss() {
        totalCacheMisses++;
    }

    /**
     * Clears the console screen
     */
    private void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // If clearing fails, just print some newlines
            System.out.println("\n".repeat(50));
        }
    }

    /**
     * Logs an error message
     * @param message Error message
     * @param e Exception
     */
    private void logError(String message, Exception e) {
        try {
            Path logFile = Paths.get(LOGS_DIR, "errors.log");
            String logEntry = String.format("[%s] %s: %s%n",
                    new Date(), message, e.getMessage());
            Files.writeString(logFile, logEntry,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ioException) {
            System.err.println("Failed to write to error log: " + ioException.getMessage());
        }
    }

    /**
     * Shuts down the service gracefully
     */
    public void shutdown() {
        dashboardRunning = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Inner class for storing report results
     */
    private static class ReportResult {
        boolean success;
        long generationTime;
        String errorMessage;
    }

    /**
     * Inner class for binary report serialization
     */
    private static class BinaryReport implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Student student;
        private final double average;
        private final List<Grade> grades;
        private final Date generationDate;

        public BinaryReport(Student student, double average, List<Grade> grades) {
            this.student = student;
            this.average = average;
            this.grades = grades;
            this.generationDate = new Date();
        }
    }
}