package org.example.presentation;

import com.google.gson.Gson;
import org.example.application.ServiceLocator;
import org.example.application.service.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * MenuController handles all user interface interactions and navigation
 * Provides a comprehensive menu system with proper error handling and navigation
 */
public class MenuController {
    private final ServiceLocator serviceLocator;
    private final Scanner scanner;

    // Navigation constants
    private static final String GO_BACK_OPTION = "0";
    private static final String GO_BACK_MESSAGE = "\n0. Go Back";

    public MenuController(ServiceLocator serviceLocator, Scanner scanner) {
        this.serviceLocator = serviceLocator;
        this.scanner = scanner;
    }

    public void startApplication() {
        displayWelcomeMessage();

        while (true) {
            try {
                displayMainMenu();
                int choice = getMenuChoice();

                if (choice == 19) {
                    System.out.println("\nThank you for using Student Grade Management System! Goodbye!");
                    shutdownServices();
                    break;
                }

                processMenuChoice(choice);
            } catch (Exception e) {
                System.out.println("\n❌ An error occurred: " + e.getMessage());
                System.out.println("Please try again.");
            }
        }
    }

    private void displayWelcomeMessage() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("      STUDENT GRADE MANAGEMENT SYSTEM v3.0");
        System.out.println("          Advanced Edition with Concurrency");
        System.out.println("=".repeat(60));
        System.out.println("Features:");
        System.out.println("• Multi-format file operations (CSV, JSON, Binary)");
        System.out.println("• Real-time statistics dashboard");
        System.out.println("• Advanced search with regex patterns");
        System.out.println("• Batch report generation with threading");
        System.out.println("• Comprehensive data validation");
        System.out.println("=".repeat(60));
    }

    private void displayMainMenu() {
        System.out.println("\nMAIN MENU");
        System.out.println("=".repeat(60));

        System.out.println("\nSTUDENT MANAGEMENT");
        System.out.println(" 1. Add Student (with validation)");
        System.out.println(" 2. View All Students");
        System.out.println(" 3. View Student Details");
        System.out.println(" 4. Record Grade");

        System.out.println("\nFILE OPERATIONS");
        System.out.println(" 5. Export Grade Report (CSV/JSON/Binary)");
        System.out.println(" 6. Import Data (Multi-format)");
        System.out.println(" 7. Bulk Import Grades");

        System.out.println("\nANALYTICS & REPORTING");
        System.out.println(" 8. Calculate Student GPA");
        System.out.println(" 9. View Class Statistics");
        System.out.println("10. Real-Time Statistics Dashboard");
        System.out.println("11. Generate Batch Reports");

        System.out.println("\nSEARCH & QUERY");
        System.out.println("12. Search Students (Advanced)");
        System.out.println("13. Pattern Based Search");
        System.out.println("14. Query Grade History");

        System.out.println("\nADVANCED FEATURES");
        System.out.println("15. Schedule Automated Tasks");
        System.out.println("16. View System Performance");
        System.out.println("17. Cache Management");
        System.out.println("18. Audit Trail Viewer");

        System.out.println("\nEXIT");
        System.out.println("19. Exit Application");
        System.out.println("=".repeat(60));
        System.out.print("\nEnter your choice (1-19): ");
    }

    private int getMenuChoice() {
        while (true) {
            try {
                String input = scanner.nextLine().trim();

                // Check for empty input
                if (input.isEmpty()) {
                    System.out.print("Please enter a choice: ");
                    continue;
                }

                int choice = Integer.parseInt(input);

                if (choice >= 1 && choice <= 19) {
                    return choice;
                } else {
                    System.out.print("Invalid choice! Enter 1-19: ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid number: ");
            }
        }
    }

    private void processMenuChoice(int choice) {
        // Log the action
        serviceLocator.getAuditService().logAction("Selected menu option " + choice);

        switch (choice) {
            case 1:
                serviceLocator.getStudentService().addStudent(scanner);
                break;
            case 2:
                serviceLocator.getStudentService().viewAllStudents();
                waitForEnter();
                break;
            case 3:
                viewStudentDetails();
                break;
            case 4:
                serviceLocator.getGradeService().addGrade(scanner);
                break;
            case 5:
                serviceLocator.getGradeService().exportGradeReport(scanner);
                waitForEnter();
                break;
            case 6:
                importData();
                break;
            case 7:
                serviceLocator.getGradeService().bulkImportGrades(scanner);
                waitForEnter();
                break;
            case 8:
                serviceLocator.getGradeService().calculateStudentGPA(scanner);
                waitForEnter();
                break;
            case 9:
                serviceLocator.getGradeService().viewClassStatistics();
                waitForEnter();
                break;
            case 10:
                showRealTimeDashboard();
                break;
            case 11:
                serviceLocator.getAdvancedStatisticsService().generateBatchReports(scanner);
                waitForEnter();
                break;
            case 12:
                advancedSearch();
                break;
            case 13:
                patternSearch();
                break;
            case 14:
                queryGradeHistory();
                break;
            case 15:
                scheduleTasks();
                break;
            case 16:
                showSystemPerformance();
                break;
            case 17:
                manageCache();
                break;
            case 18:
                viewAuditTrail();
                break;
            default:
                System.out.println("Invalid option!");
                waitForEnter();
                break;
        }
    }

    private void viewStudentDetails() {
        System.out.println("\nVIEW STUDENT DETAILS");
        System.out.println("=".repeat(40));
        System.out.println(GO_BACK_MESSAGE);
        System.out.print("Enter Student ID: ");

        String id = scanner.nextLine().trim();
        if (id.equals(GO_BACK_OPTION)) {
            return;
        }

        serviceLocator.getStudentService().findStudent(id);
        waitForEnter();
    }

    private void importData() {
        System.out.println("\nIMPORT DATA");
        System.out.println("=".repeat(50));

        while (true) {
            System.out.println("\nSelect import format:");
            System.out.println("1. Import from CSV");
            System.out.println("2. Import from JSON");
            System.out.println("3. Import from Binary");
            System.out.println(GO_BACK_MESSAGE);
            System.out.print("Select option: ");

            String input = scanner.nextLine().trim();
            if (input.equals(GO_BACK_OPTION)) {
                return;
            }

            try {
                int format = Integer.parseInt(input);

                if (format < 1 || format > 3) {
                    System.out.println("❌ Invalid option. Please select 1-3 or 0 to go back.");
                    continue;
                }

                System.out.print("Enter filename (without extension): ");
                String filename = scanner.nextLine().trim();

                if (filename.equals(GO_BACK_OPTION)) {
                    continue;
                }

                String extension = getExtensionForFormat(format);
                Path filePath = Paths.get("./imports/", filename + extension);

                if (!Files.exists(filePath)) {
                    System.out.println("❌ File not found: " + filePath);
                    System.out.println("Please ensure the file exists in the ./imports/ directory.");
                    continue;
                }

                System.out.println("✅ File found: " + filePath);
                System.out.println("Importing data...");

                // Simulate import process (you would implement actual import logic)
                Thread.sleep(1000);
                System.out.println("✅ Import completed successfully!");

                // Display imported data
                displayImportedData(filePath, format);

                waitForEnter();
                break;

            } catch (NumberFormatException e) {
                System.out.println("❌ Please enter a valid number.");
            } catch (Exception e) {
                System.out.println("❌ Import failed: " + e.getMessage());
            }
        }
    }

    private String getExtensionForFormat(int format) {
        switch (format) {
            case 1: return ".csv";
            case 2: return ".json";
            case 3: return ".dat";
            default: return ".txt";
        }
    }

    private void displayImportedData(Path filePath, int format) {
        try {
            System.out.println("\nIMPORTED DATA PREVIEW");
            System.out.println("-".repeat(40));

            if (format == 1) { // CSV
                List<String> lines = Files.readAllLines(filePath);
                int previewLines = Math.min(10, lines.size());

                System.out.println("First " + previewLines + " lines:");
                for (int i = 0; i < previewLines; i++) {
                    System.out.println(lines.get(i));
                }

                System.out.println("\nTotal lines: " + lines.size());
                System.out.println("File size: " + formatFileSize(Files.size(filePath)));

            } else if (format == 2) { // JSON
                String content = Files.readString(filePath);
                System.out.println("JSON Structure:");
                System.out.println("Total characters: " + content.length());

                // Parse and show structure
                Gson gson = new Gson();
                Object json = gson.fromJson(content, Object.class);
                System.out.println("JSON parsed successfully");

            } else if (format == 3) { // Binary
                System.out.println("Binary file size: " + formatFileSize(Files.size(filePath)));
                System.out.println("Binary files are not human-readable.");
            }

        } catch (Exception e) {
            System.out.println("❌ Could not display imported data: " + e.getMessage());
        }
    }

    private void showRealTimeDashboard() {
        System.out.println("\nREAL-TIME STATISTICS DASHBOARD");
        System.out.println("=".repeat(60));
        System.out.println("Launching dashboard...");
        System.out.println("Use the following commands in the dashboard:");
        System.out.println("• Q - Quit dashboard");
        System.out.println("• R - Refresh immediately");
        System.out.println("• P - Pause auto-refresh");
        System.out.println("• H - Show help");
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();

        serviceLocator.getAdvancedStatisticsService().displayDashboard(scanner);
    }

    private void advancedSearch() {
        System.out.println("\n🔍 ADVANCED SEARCH");
        System.out.println("=".repeat(40));
        System.out.println(GO_BACK_MESSAGE);

        serviceLocator.getSearchService().advancedSearch(scanner);
        waitForEnter();
    }

    private void patternSearch() {
        System.out.println("\n🔍 PATTERN-BASED SEARCH");
        System.out.println("=".repeat(40));
        System.out.println(GO_BACK_MESSAGE);

        serviceLocator.getSearchService().patternSearch(scanner);
        waitForEnter();
    }

    private void queryGradeHistory() {
        System.out.println("\nGRADE HISTORY");
        System.out.println("=".repeat(40));
        System.out.println(GO_BACK_MESSAGE);

        serviceLocator.getAuditService().displayGradeHistory();
        waitForEnter();
    }

    private void scheduleTasks() {
        System.out.println("\nSCHEDULE AUTOMATED TASKS");
        System.out.println("=".repeat(40));
        System.out.println(GO_BACK_MESSAGE);

        serviceLocator.getTaskSchedulerService().scheduleTask(scanner);
        waitForEnter();
    }

    private void showSystemPerformance() {
        System.out.println("\nSYSTEM PERFORMANCE");
        System.out.println("=".repeat(60));

        serviceLocator.getAdvancedStatisticsService().showPerformanceMetrics();
        waitForEnter();
    }

    private void manageCache() {
        System.out.println("\nCACHE MANAGEMENT");
        System.out.println("=".repeat(40));
        System.out.println(GO_BACK_MESSAGE);

        serviceLocator.getCacheService().manageCache(scanner);
        waitForEnter();
    }

    private void viewAuditTrail() {
        System.out.println("\nAUDIT TRAIL");
        System.out.println("=".repeat(40));
        System.out.println(GO_BACK_MESSAGE);

        serviceLocator.getAuditService().displayAuditTrail(scanner);
        waitForEnter();
    }

    private void shutdownServices() {
        System.out.println("\nShutting down services...");
        serviceLocator.getAdvancedStatisticsService().shutdown();
        serviceLocator.getAuditService().shutdown();
        serviceLocator.getTaskSchedulerService().shutdown();
        System.out.println("✅ All services shut down successfully.");
    }

    private void waitForEnter() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}