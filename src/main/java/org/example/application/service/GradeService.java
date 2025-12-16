package org.example.application.service;

import org.example.domain.model.*;
import org.example.domain.repository.*;
import org.example.infrastructure.util.*;

import java.util.*;
import java.util.concurrent.*;

public class GradeService {
    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final ValidationService validationService;
    private final ExportService exportService;
    private final StatisticsService statisticsService;

    private final Subject[] subjects = {
            new CoreSubject("Mathematics", "MAT101"),
            new CoreSubject("English", "ENG101"),
            new CoreSubject("Science", "SCI101"),
            new ElectiveSubject("Music", "MUS101"),
            new ElectiveSubject("Art", "ART101"),
            new ElectiveSubject("Physical Education", "PE101")
    };

    private final TreeMap<Double, List<Student>> gpaRankings = new TreeMap<>(Collections.reverseOrder());
    private final HashSet<String> uniqueCourses = new HashSet<>();
    private final LinkedList<GradeHistoryEntry> gradeHistory = new LinkedList<>();
    private final PriorityQueue<GradeTask> taskQueue = new PriorityQueue<>(new GradeTaskComparator());

    public GradeService(GradeRepository gradeRepository,
                        StudentRepository studentRepository,
                        ValidationService validationService,
                        ExportService exportService,
                        StatisticsService statisticsService) {
        this.gradeRepository = gradeRepository;
        this.studentRepository = studentRepository;
        this.validationService = validationService;
        this.exportService = exportService;
        this.statisticsService = statisticsService;

        initializeUniqueCourses();
    }

    private void initializeUniqueCourses() {
        for (Subject subject : subjects) {
            uniqueCourses.add(subject.getSubjectCode());
        }
    }

    public void addGrade(Scanner scanner) {
        scheduleTask(new GradeTask("Process new grade", 3, () ->
                System.out.println("Processing grade task...")));

        System.out.println("\nRECORD GRADE");
        System.out.println("=".repeat(30));

        String studentId = promptForStudentId(scanner);
        Student student = studentRepository.findById(studentId);

        if (student == null) {
            System.out.println("Student not found!");
            return;
        }

        displayStudentInfo(student);
        Subject selectedSubject = promptForSubject(scanner);
        double gradeValue = promptForGradeValue(scanner);

        processGradeSubmission(scanner, student, selectedSubject, gradeValue);
    }

    private String promptForStudentId(Scanner scanner) {
        System.out.print("Enter Student ID: ");
        return scanner.nextLine().trim();
    }

    private void displayStudentInfo(Student student) {
        System.out.println("\nStudent Details:");
        System.out.println("-".repeat(40));
        System.out.println("Name: " + student.getName());
        System.out.println("Type: " + student.getStudentType());
        System.out.printf("Current Average: %.1f%%\n",
                gradeRepository.calculateStudentAverage(student.getStudentId()));
    }

    private Subject promptForSubject(Scanner scanner) {
        System.out.println("\nSubject Type:");
        System.out.println("1. Core Subject");
        System.out.println("2. Elective Subject");
        System.out.print("Select (1-2): ");

        int choice = getValidChoice(scanner, 1, 2);
        List<Subject> filteredSubjects = filterSubjectsByType(choice);

        System.out.println("\nAvailable Subjects:");
        for (int i = 0; i < filteredSubjects.size(); i++) {
            System.out.println((i + 1) + ". " + filteredSubjects.get(i).getSubjectName());
        }

        System.out.print("Select subject (1-" + filteredSubjects.size() + "): ");
        int subjectChoice = getValidChoice(scanner, 1, filteredSubjects.size());

        return filteredSubjects.get(subjectChoice - 1);
    }

    private List<Subject> filterSubjectsByType(int type) {
        List<Subject> filtered = new ArrayList<>();
        String targetType = (type == 1) ? "Core" : "Elective";

        for (Subject subject : subjects) {
            if (subject.getSubjectType().equals(targetType)) {
                filtered.add(subject);
            }
        }
        return filtered;
    }

    private double promptForGradeValue(Scanner scanner) {
        System.out.print("Enter grade (0-100): ");

        while (true) {
            try {
                double grade = Double.parseDouble(scanner.nextLine().trim());
                if (grade >= 0 && grade <= 100) return grade;
                System.out.print("Invalid! Enter grade between 0-100: ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid! Enter a number: ");
            }
        }
    }

    private void processGradeSubmission(Scanner scanner, Student student,
                                        Subject subject, double gradeValue) {
        Grade existingGrade = gradeRepository.findGradeByStudentAndSubject(
                student.getStudentId(), subject.getSubjectCode());

        if (existingGrade != null) {
            handleGradeUpdate(scanner, student, subject, gradeValue, existingGrade);
            return;
        }

        createNewGrade(scanner, student, subject, gradeValue);
    }

    private void handleGradeUpdate(Scanner scanner, Student student, Subject subject,
                                   double newGrade, Grade existingGrade) {
        System.out.println("\nGrade already exists!");
        System.out.println("Current: " + existingGrade.getGrade() + "%");
        System.out.print("Update? (Y/N): ");

        if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("Update cancelled.");
            return;
        }

        logGradeHistory(student.getStudentId(), subject.getSubjectCode(),
                existingGrade.getGrade(), newGrade, "UPDATE");

        existingGrade.setGrade(newGrade);
        gradeRepository.update(existingGrade);

        System.out.println("✅ Grade updated successfully!");
        updateGpaRankings(student);
    }

    private void createNewGrade(Scanner scanner, Student student,
                                Subject subject, double gradeValue) {
        Grade newGrade = new Grade(student.getStudentId(), subject, gradeValue);
        newGrade.setGradeId(String.format("GRD%03d", gradeRepository.count() + 1));

        System.out.println("\nGRADE CONFIRMATION");
        System.out.println("-".repeat(40));
        newGrade.displayGradeDetails();
        System.out.print("Confirm? (Y/N): ");

        if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("Grade recording cancelled.");
            return;
        }

        gradeRepository.save(newGrade);
        logGradeHistory(student.getStudentId(), subject.getSubjectCode(),
                0, gradeValue, "ADD");
        updateGpaRankings(student);

        System.out.println("✅ Grade saved successfully!");
        processTasks();
    }

    private void updateGpaRankings(Student student) {
        double gpa = gradeRepository.calculateStudentAverage(student.getStudentId());

        gpaRankings.values().forEach(list -> list.removeIf(
                s -> s.getStudentId().equals(student.getStudentId())));

        gpaRankings.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        gpaRankings.computeIfAbsent(gpa, k -> new ArrayList<>()).add(student);
    }

    private void logGradeHistory(String studentId, String subjectCode,
                                 double oldGrade, double newGrade, String action) {
        gradeHistory.addFirst(new GradeHistoryEntry(
                studentId, subjectCode, oldGrade, newGrade, action));

        if (gradeHistory.size() > 1000) {
            gradeHistory.removeLast();
        }
    }

    private void scheduleTask(GradeTask task) {
        taskQueue.add(task);
    }

    private void processTasks() {
        System.out.println("\nProcessing scheduled tasks...");
        int processed = 0;

        while (!taskQueue.isEmpty() && processed < 3) {
            GradeTask task = taskQueue.poll();
            System.out.println("  ✓ " + task.getDescription());
            if (task.getAction() != null) {
                task.getAction().run();
            }
            processed++;
        }
    }

    public void exportGradeReport(Scanner scanner) {
        System.out.println("\n📊 EXPORT GRADE REPORT");
        System.out.println("=".repeat(60));

        ExportOptions options = gatherExportOptions(scanner);
        ExportResult result = performMultiFormatExport(options);

        displayExportSummary(result);
    }

    private ExportOptions gatherExportOptions(Scanner scanner) {
        ExportOptions options = new ExportOptions();

        System.out.print("Enter Student ID (or 'ALL'): ");
        options.studentId = scanner.nextLine().trim();

        System.out.println("\nReport Type:");
        System.out.println("1. Summary Report");
        System.out.println("2. Detailed Report");
        System.out.println("3. Transcript Format");
        System.out.println("4. Performance Analytics");
        options.reportType = getValidChoice(scanner, 1, 4);

        System.out.println("\nExport Format:");
        System.out.println("1. CSV");
        System.out.println("2. JSON");
        System.out.println("3. Binary");
        System.out.println("4. All Formats");
        int formatChoice = getValidChoice(scanner, 1, 4);

        options.formats = getFormatsFromChoice(formatChoice);

        return options;
    }

    private List<String> getFormatsFromChoice(int choice) {
        switch (choice) {
            case 1: return Arrays.asList("CSV");
            case 2: return Arrays.asList("JSON");
            case 3: return Arrays.asList("BINARY");
            case 4: return Arrays.asList("CSV", "JSON", "BINARY");
            default: return Arrays.asList("CSV");
        }
    }

    private ExportResult performMultiFormatExport(ExportOptions options) {
        ExportResult result = new ExportResult();
        ExecutorService executor = Executors.newFixedThreadPool(options.formats.size());
        List<Future<FormatExportResult>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (String format : options.formats) {
            futures.add(executor.submit(() ->
                    exportSingleFormat(options, format)));
        }

        executor.shutdown();

        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);

            for (Future<FormatExportResult> future : futures) {
                FormatExportResult formatResult = future.get();
                if (formatResult.success) {
                    result.completedFormats++;
                    result.totalSize += formatResult.fileSize;
                    result.formatResults.add(formatResult);
                }
            }
        } catch (Exception e) {
            System.out.println("Export error: " + e.getMessage());
        }

        result.totalTime = System.currentTimeMillis() - startTime;
        return result;
    }

    private FormatExportResult exportSingleFormat(ExportOptions options, String format) {
        FormatExportResult result = new FormatExportResult();
        result.format = format;

        try {
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            String fileName = (options.studentId.equals("ALL") ? "ALL_STUDENTS" : options.studentId)
                    + "_" + getReportTypeName(options.reportType) + "_" + timestamp;

            java.nio.file.Path filePath = java.nio.file.Paths.get("./reports",
                    fileName + getFileExtension(format));

            long startTime = System.currentTimeMillis();

            switch (format) {
                case "CSV": exportToCSV(options, filePath); break;
                case "JSON": exportToJSON(options, filePath); break;
                case "BINARY": exportToBinary(options, filePath); break;
            }

            result.filePath = filePath.toString();
            result.fileSize = java.nio.file.Files.size(filePath);
            result.exportTime = System.currentTimeMillis() - startTime;
            result.success = true;

        } catch (Exception e) {
            result.success = false;
            result.error = e.getMessage();
        }

        return result;
    }

    private void exportToCSV(ExportOptions options, java.nio.file.Path filePath)
            throws java.io.IOException {
        List<String> lines = new ArrayList<>();

        if (options.reportType == 1) {
            lines.add("StudentID,Name,Type,AverageGrade,Subjects,Status");
            List<Student> students = getStudentsForExport(options);

            for (Student student : students) {
                double avg = gradeRepository.calculateStudentAverage(student.getStudentId());
                int subs = gradeRepository.getStudentSubjectCount(student.getStudentId());
                String status = avg >= student.getPassingGrade() ? "PASSING" : "FAILING";

                lines.add(String.format("%s,%s,%s,%.2f,%d,%s",
                        student.getStudentId(), student.getName(), student.getStudentType(),
                        avg, subs, status));
            }
        }

        java.nio.file.Files.write(filePath, lines,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void exportToJSON(ExportOptions options, java.nio.file.Path filePath)
            throws java.io.IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"exportDate\": \"").append(java.time.LocalDateTime.now()).append("\",\n");
        json.append("  \"reportType\": \"").append(getReportTypeName(options.reportType)).append("\",\n");

        List<Student> students = getStudentsForExport(options);
        json.append("  \"totalStudents\": ").append(students.size()).append(",\n");
        json.append("  \"students\": [\n");

        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            json.append("    {\n");
            json.append("      \"id\": \"").append(student.getStudentId()).append("\",\n");
            json.append("      \"name\": \"").append(student.getName()).append("\",\n");
            json.append("      \"type\": \"").append(student.getStudentType()).append("\",\n");

            if (options.reportType == 1) {
                double avg = gradeRepository.calculateStudentAverage(student.getStudentId());
                json.append("      \"averageGrade\": ").append(avg).append(",\n");
                json.append("      \"subjects\": ").append(gradeRepository.getStudentSubjectCount(student.getStudentId())).append("\n");
            }

            json.append("    }").append(i < students.size() - 1 ? "," : "").append("\n");
        }

        json.append("  ]\n");
        json.append("}");

        java.nio.file.Files.writeString(filePath, json.toString(),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
    }

    private void exportToBinary(ExportOptions options, java.nio.file.Path filePath)
            throws java.io.IOException {
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(
                new java.io.BufferedOutputStream(
                        java.nio.file.Files.newOutputStream(filePath)))) {

            java.util.Map<String, Object> exportData = new java.util.HashMap<>();
            exportData.put("exportDate", java.time.LocalDateTime.now());
            exportData.put("reportType", getReportTypeName(options.reportType));

            List<Student> students = getStudentsForExport(options);
            exportData.put("studentCount", students.size());

            List<java.util.Map<String, Object>> studentData = new ArrayList<>();
            for (Student student : students) {
                java.util.Map<String, Object> sData = new java.util.HashMap<>();
                sData.put("id", student.getStudentId());
                sData.put("name", student.getName());
                sData.put("type", student.getStudentType());
                sData.put("averageGrade", gradeRepository.calculateStudentAverage(student.getStudentId()));
                studentData.add(sData);
            }
            exportData.put("students", studentData);

            oos.writeObject(exportData);
        }
    }

    private List<Student> getStudentsForExport(ExportOptions options) {
        if (options.studentId.equals("ALL")) {
            return studentRepository.findAll();
        } else {
            Student student = studentRepository.findById(options.studentId);
            return student != null ? Arrays.asList(student) : new ArrayList<>();
        }
    }

    private void displayExportSummary(ExportResult result) {
        System.out.println("\n✅ EXPORT COMPLETED");
        System.out.println("=".repeat(60));

        for (int i = 0; i < result.formatResults.size(); i++) {
            FormatExportResult formatResult = result.formatResults.get(i);
            System.out.println("\n" + (i + 1) + ". " + formatResult.format + " Export:");
            System.out.println("   └─ File: " + java.nio.file.Paths.get(formatResult.filePath).getFileName());
            System.out.println("   └─ Location: " + formatResult.filePath);
            System.out.println("   └─ Size: " + formatFileSize(formatResult.fileSize));
            System.out.println("   └─ Export Time: " + formatResult.exportTime + "ms");
        }

        System.out.println("\n📈 PERFORMANCE SUMMARY:");
        System.out.println("   └─ Total Time: " + result.totalTime + "ms");
        System.out.println("   └─ Total Size: " + formatFileSize(result.totalSize));
        System.out.println("   └─ Completed: " + result.completedFormats + "/" + result.formatResults.size() + " formats");
        System.out.println("   └─ I/O Operations: " + result.formatResults.size() + " parallel writes");

        if (result.totalTime > 0) {
            double throughput = (result.totalSize / 1024.0) / (result.totalTime / 1000.0);
            System.out.printf("   └─ Throughput: %.2f KB/s%n", throughput);
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String getReportTypeName(int type) {
        switch (type) {
            case 1: return "SUMMARY";
            case 2: return "DETAILED";
            case 3: return "TRANSCRIPT";
            case 4: return "ANALYTICS";
            default: return "UNKNOWN";
        }
    }

    private String getFileExtension(String format) {
        switch (format) {
            case "CSV": return ".csv";
            case "JSON": return ".json";
            case "BINARY": return ".bin";
            default: return ".txt";
        }
    }

    private int getValidChoice(Scanner scanner, int min, int max) {
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= min && choice <= max) return choice;
                System.out.print("Enter number between " + min + " and " + max + ": ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a number: ");
            }
        }
    }

    // Inner classes for export functionality
    private static class ExportOptions {
        String studentId;
        int reportType;
        List<String> formats;
    }

    private static class ExportResult {
        long totalTime;
        long totalSize;
        int completedFormats;
        List<FormatExportResult> formatResults = new ArrayList<>();
    }

    private static class FormatExportResult {
        String format;
        String filePath;
        long fileSize;
        long exportTime;
        boolean success;
        String error;
    }

    // Other methods for the original functionality...
    public void calculateStudentGPA(Scanner scanner) {
        System.out.println("\nCALCULATE STUDENT GPA");
        System.out.println("=".repeat(30));

        System.out.print("Enter Student ID: ");
        String id = scanner.nextLine().trim();

        Student student = studentRepository.findById(id);
        if (student == null) {
            System.out.println("Student not found!");
            return;
        }

        List<Grade> grades = gradeRepository.findByStudentId(id);
        if (grades.isEmpty()) {
            System.out.println("No grades recorded.");
            return;
        }

        System.out.println("\nGPA Report for " + student.getName());
        System.out.println("-".repeat(40));

        double totalGPA = 0;
        for (Grade grade : grades) {
            double gpa = GPACalculator.percentageToGPA(grade.getGrade());
            String letter = GPACalculator.gpaToLetter(gpa);
            totalGPA += gpa;

            System.out.printf("%-20s: %.1f%% -> %.2f (%s)%n",
                    grade.getSubject().getSubjectName(), grade.getGrade(), gpa, letter);
        }

        double cumulativeGPA = totalGPA / grades.size();
        System.out.println("-".repeat(40));
        System.out.printf("Cumulative GPA: %.2f / 4.0%n", cumulativeGPA);
    }

    public void viewClassStatistics() {
        statisticsService.generateClassReport(studentRepository, gradeRepository);
    }

    public void searchStudents(Scanner scanner) {
        System.out.println("\nSEARCH STUDENTS");
        System.out.println("=".repeat(30));

        System.out.print("Enter search term: ");
        String term = scanner.nextLine().trim().toLowerCase();

        List<Student> results = studentRepository.findAll().stream()
                .filter(s -> s.getName().toLowerCase().contains(term) ||
                        s.getStudentId().toLowerCase().contains(term))
                .collect(java.util.stream.Collectors.toList());

        if (results.isEmpty()) {
            System.out.println("No students found.");
            return;
        }

        System.out.println("\nSearch Results (" + results.size() + " found):");
        System.out.println("-".repeat(80));

        for (Student student : results) {
            double avg = gradeRepository.calculateStudentAverage(student.getStudentId());
            System.out.printf("%-12s | %-25s | %-15s | %.1f%%%n",
                    student.getStudentId(), student.getName(), student.getStudentType(), avg);
        }
    }

    public void bulkImportGrades(Scanner scanner) {
        System.out.println("\nBULK IMPORT GRADES");
        System.out.println("=".repeat(30));

        System.out.println("This feature requires a CSV file in ./imports/ directory.");
        System.out.println("Format: StudentID,SubjectCode,Grade");
        System.out.print("Enter filename (without .csv): ");
        String filename = scanner.nextLine().trim();

        java.nio.file.Path filePath = java.nio.file.Paths.get("./imports", filename + ".csv");

        if (!java.nio.file.Files.exists(filePath)) {
            System.out.println("File not found: " + filePath);
            return;
        }

        try {
            List<String> lines = java.nio.file.Files.readAllLines(filePath);
            int imported = 0;
            int failed = 0;

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length != 3) {
                    failed++;
                    continue;
                }

                String studentId = parts[0].trim();
                String subjectCode = parts[1].trim();
                double grade = Double.parseDouble(parts[2].trim());

                Student student = studentRepository.findById(studentId);
                if (student == null) {
                    failed++;
                    continue;
                }

                Subject subject = findSubjectByCode(subjectCode);
                if (subject == null) {
                    failed++;
                    continue;
                }

                Grade newGrade = new Grade(studentId, subject, grade);
                gradeRepository.save(newGrade);
                imported++;
            }

            System.out.println("Import completed: " + imported + " imported, " + failed + " failed");

        } catch (Exception e) {
            System.out.println("Import failed: " + e.getMessage());
        }
    }

    private Subject findSubjectByCode(String code) {
        for (Subject subject : subjects) {
            if (subject.getSubjectCode().equals(code)) {
                return subject;
            }
        }
        return null;
    }

    public void viewGradeByStudent(Scanner scanner) {
        System.out.println("\nVIEW GRADE BY STUDENT");
        System.out.println("=".repeat(30));

        System.out.print("Enter Student ID: ");
        String id = scanner.nextLine().trim();

        Student student = studentRepository.findById(id);
        if (student == null) {
            System.out.println("Student not found!");
            return;
        }

        List<Grade> grades = gradeRepository.findByStudentId(id);
        if (grades.isEmpty()) {
            System.out.println("No grades recorded for this student.");
            return;
        }

        System.out.println("\nGrades for " + student.getName() + " (" + student.getStudentId() + "):");
        System.out.println("-".repeat(60));
        System.out.printf("%-20s | %-12s | %-10s%n", "Subject", "Type", "Grade");
        System.out.println("-".repeat(60));

        for (Grade grade : grades) {
            System.out.printf("%-20s | %-12s | %-9.1f%%%n",
                    grade.getSubject().getSubjectName(),
                    grade.getSubject().getSubjectType(),
                    grade.getGrade());
        }

        double average = gradeRepository.calculateStudentAverage(id);
        System.out.println("-".repeat(60));
        System.out.printf("Average: %.1f%% | Status: %s%n",
                average, average >= student.getPassingGrade() ? "Passing" : "Failing");
    }
}

// Additional inner classes for data structures
class GradeHistoryEntry {
    private final String studentId;
    private final String subjectCode;
    private final double oldGrade;
    private final double newGrade;
    private final String action;
    private final java.util.Date timestamp;

    public GradeHistoryEntry(String studentId, String subjectCode,
                             double oldGrade, double newGrade, String action) {
        this.studentId = studentId;
        this.subjectCode = subjectCode;
        this.oldGrade = oldGrade;
        this.newGrade = newGrade;
        this.action = action;
        this.timestamp = new java.util.Date();
    }

    public String getStudentId() { return studentId; }
    public String getSubjectCode() { return subjectCode; }
    public double getOldGrade() { return oldGrade; }
    public double getNewGrade() { return newGrade; }
    public String getAction() { return action; }
    public java.util.Date getTimestamp() { return timestamp; }
}

class GradeTask {
    private final String description;
    private final int priority;
    private final Runnable action;
    private final java.util.Date scheduledTime;

    public GradeTask(String description, int priority, Runnable action) {
        this.description = description;
        this.priority = priority;
        this.action = action;
        this.scheduledTime = new java.util.Date();
    }

    public String getDescription() { return description; }
    public int getPriority() { return priority; }
    public Runnable getAction() { return action; }
    public java.util.Date getScheduledTime() { return scheduledTime; }
}

class GradeTaskComparator implements java.util.Comparator<GradeTask> {
    @Override
    public int compare(GradeTask t1, GradeTask t2) {
        return Integer.compare(t2.getPriority(), t1.getPriority());
    }
}