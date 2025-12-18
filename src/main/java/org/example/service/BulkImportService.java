package org.example.service;

import org.example.application.service.ValidationService;
import org.example.domain.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.domain.repository.GradeRepository;
import org.example.domain.repository.StudentRepository;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * BulkImportService handles importing grades from multiple file formats
 * Supports CSV, JSON, and Binary formats
 */
public class BulkImportService {
    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final ValidationService validationService;
    private final Gson gson;

    private static final String IMPORTS_DIR = "./imports/";
    private static final String REPORTS_DIR = "./reports/";
    private final ExecutorService importExecutor;

    static {
        // Create necessary directories
        createDirectories();
    }

    private static void createDirectories() {
        try {
            Files.createDirectories(Paths.get(IMPORTS_DIR));
            Files.createDirectories(Paths.get(REPORTS_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create directories: " + e.getMessage());
        }
    }

    public BulkImportService(StudentRepository studentRepository,
                             GradeRepository gradeRepository,
                             ValidationService validationService) {
        this.studentRepository = studentRepository;
        this.gradeRepository = gradeRepository;
        this.validationService = validationService;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.importExecutor = Executors.newFixedThreadPool(3);
    }

    /**
     * Main import method that detects format and imports grades
     */
    public void bulkImportGrades(String filename) {
        System.out.println("\n📥 BULK IMPORT");
        System.out.println("=".repeat(60));

        Path filePath = Paths.get(IMPORTS_DIR + filename);
        if (!Files.exists(filePath)) {
            System.out.println("❌ File not found: " + filePath);
            return;
        }

        try {
            FileFormat format = detectFileFormat(filename);
            System.out.println("Detected format: " + format);

            List<GradeImportRecord> importRecords;

            switch (format) {
                case CSV:
                    importRecords = importFromCSV(filePath);
                    break;
                case JSON:
                    importRecords = importFromJSON(filePath);
                    break;
                case BINARY:
                    importRecords = importFromBinary(filePath);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported format: " + format);
            }

            System.out.println("✅ Successfully read " + importRecords.size() + " records");

            // Process imports with progress tracking
            processImportRecords(importRecords);

        } catch (Exception e) {
            System.out.println("❌ Import failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Detect file format based on extension
     */
    private FileFormat detectFileFormat(String filename) {
        String lowerName = filename.toLowerCase();

        if (lowerName.endsWith(".csv")) {
            return FileFormat.CSV;
        } else if (lowerName.endsWith(".json")) {
            return FileFormat.JSON;
        } else if (lowerName.endsWith(".bin") || lowerName.endsWith(".dat")) {
            return FileFormat.BINARY;
        } else {
            throw new IllegalArgumentException("Unknown file format. Supported: .csv, .json, .bin, .dat");
        }
    }

    /**
     * Import grades from CSV file
     */
    private List<GradeImportRecord> importFromCSV(Path filePath) throws IOException {
        List<GradeImportRecord> records = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                String[] parts = line.split(",");
                if (parts.length < 3) {
                    System.out.println("⚠️ Line " + lineNumber + ": Invalid format, skipping");
                    continue;
                }

                try {
                    GradeImportRecord record = new GradeImportRecord();
                    record.studentId = parts[0].trim();
                    record.subjectCode = parts[1].trim();
                    record.grade = Double.parseDouble(parts[2].trim());

                    // Optional fields
                    if (parts.length > 3) {
                        record.date = parts[3].trim();
                    }
                    if (parts.length > 4) {
                        record.subjectType = parts[4].trim();
                    }

                    records.add(record);
                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Line " + lineNumber + ": Invalid grade format, skipping");
                }
            }
        }

        return records;
    }

    /**
     * Import grades from JSON file
     */
    private List<GradeImportRecord> importFromJSON(Path filePath) throws IOException {
        String jsonContent = Files.readString(filePath);

        // Try to parse as array
        try {
            return gson.fromJson(jsonContent, new TypeToken<List<GradeImportRecord>>(){}.getType());
        } catch (Exception e) {
            // Try alternative format with wrapper
            try {
                JsonImportWrapper wrapper = gson.fromJson(jsonContent, JsonImportWrapper.class);
                return wrapper.grades != null ? wrapper.grades : new ArrayList<>();
            } catch (Exception e2) {
                throw new IOException("Invalid JSON format: " + e.getMessage());
            }
        }
    }

    /**
     * Import grades from Binary file
     */
    private List<GradeImportRecord> importFromBinary(Path filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(filePath))) {
            Object obj = ois.readObject();

            if (obj instanceof List) {
                @SuppressWarnings("unchecked")
                List<GradeImportRecord> records = (List<GradeImportRecord>) obj;
                return records;
            } else if (obj instanceof BinaryImportWrapper) {
                return ((BinaryImportWrapper) obj).grades;
            } else {
                throw new IOException("Invalid binary format");
            }
        }
    }

    /**
     * Process imported records with validation and threading
     */
    private void processImportRecords(List<GradeImportRecord> records) {
        System.out.println("\n🔄 PROCESSING IMPORT RECORDS");
        System.out.println("-".repeat(50));

        CountDownLatch latch = new CountDownLatch(records.size());
        List<Future<ImportResult>> futures = new ArrayList<>();

        // Track statistics
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = new CopyOnWriteArrayList<>();

        // Submit import tasks
        for (GradeImportRecord record : records) {
            futures.add(importExecutor.submit(() -> {
                try {
                    ImportResult result = processSingleRecord(record);

                    if (result.success) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                        errors.add(result.errorMessage);
                    }

                    return result;
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    errors.add("Error processing record: " + e.getMessage());
                    return new ImportResult(false, "Exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }));
        }

        // Show progress
        showImportProgress(records.size(), latch);

        // Wait for completion
        try {
            latch.await();
            importExecutor.shutdown();
            importExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Show summary
        showImportSummary(successCount.get(), failCount.get(), errors);

        // Generate import report
        generateImportReport(successCount.get(), failCount.get(), errors);
    }

    /**
     * Process a single grade import record
     */
    private ImportResult processSingleRecord(GradeImportRecord record) {
        try {
            // Validate student exists
            Student student = studentRepository.findById(record.studentId);
            if (student == null) {
                return new ImportResult(false, "Student not found: " + record.studentId);
            }

            // Validate grade
            if (record.grade < 0 || record.grade > 100) {
                return new ImportResult(false, "Invalid grade range: " + record.grade);
            }

            // Find or create subject
            Subject subject = findOrCreateSubject(record.subjectCode, record.subjectType);

            // Check if grade already exists
            Grade existingGrade = gradeRepository.findGradeByStudentAndSubject(
                    record.studentId, record.subjectCode);

            if (existingGrade != null) {
                // Update existing grade
                existingGrade.setGrade(record.grade);
                if (record.date != null) {
                    existingGrade.setDate(record.date);
                }
                gradeRepository.update(existingGrade);
                return new ImportResult(true, "Updated grade for " + student.getName());
            } else {
                // Create new grade
                Grade newGrade = new Grade(record.studentId, subject, record.grade);
                newGrade.setGradeId("GRD" + System.currentTimeMillis() + new Random().nextInt(1000));

                if (record.date != null) {
                    newGrade.setDate(record.date);
                }

                gradeRepository.save(newGrade);
                return new ImportResult(true, "Added grade for " + student.getName());
            }

        } catch (Exception e) {
            return new ImportResult(false, "Error: " + e.getMessage());
        }
    }

    /**
     * Find existing subject or create a new one
     */
    private Subject findOrCreateSubject(String subjectCode, String subjectType) {
        // This is a simplified implementation
        // In a real system, you would have a SubjectRepository

        // Common subjects
        Map<String, Subject> subjectMap = new HashMap<>();
        subjectMap.put("MAT101", new CoreSubject("Mathematics", "MAT101"));
        subjectMap.put("ENG101", new CoreSubject("English", "ENG101"));
        subjectMap.put("SCI101", new CoreSubject("Science", "SCI101"));
        subjectMap.put("MUS101", new ElectiveSubject("Music", "MUS101"));
        subjectMap.put("ART101", new ElectiveSubject("Art", "ART101"));
        subjectMap.put("PE101", new ElectiveSubject("Physical Education", "PE101"));

        Subject subject = subjectMap.get(subjectCode);
        if (subject == null) {
            // Create new subject based on type
            if ("Core".equalsIgnoreCase(subjectType)) {
                subject = new CoreSubject("Unknown", subjectCode);
            } else {
                subject = new ElectiveSubject("Unknown", subjectCode);
            }
        }

        return subject;
    }

    /**
     * Show import progress
     */
    private void showImportProgress(int total, CountDownLatch latch) {
        Thread progressThread = new Thread(() -> {
            int lastCount = total;
            try {
                while (latch.getCount() > 0) {
                    int completed = total - (int) latch.getCount();
                    if (completed != lastCount) {
                        double percentage = (completed * 100.0) / total;
                        System.out.printf("\rProgress: [%-50s] %.1f%% (%d/%d)",
                                "=".repeat((int) (percentage / 2)), percentage, completed, total);
                        lastCount = completed;
                    }
                    Thread.sleep(100);
                }
                System.out.printf("\rProgress: [%-50s] 100.0%% (%d/%d)%n",
                        "=".repeat(50), total, total);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        progressThread.setDaemon(true);
        progressThread.start();
    }

    /**
     * Show import summary
     */
    private void showImportSummary(int success, int fail, List<String> errors) {
        System.out.println("\n📊 IMPORT SUMMARY");
        System.out.println("=".repeat(50));
        System.out.println("Total Records: " + (success + fail));
        System.out.println("✅ Successful: " + success);
        System.out.println("❌ Failed: " + fail);

        if (fail > 0) {
            System.out.println("\n⚠️ ERRORS:");
            for (int i = 0; i < Math.min(5, errors.size()); i++) {
                System.out.println("  " + (i + 1) + ". " + errors.get(i));
            }
            if (errors.size() > 5) {
                System.out.println("  ... and " + (errors.size() - 5) + " more errors");
            }
        }

        double successRate = (success + fail) > 0 ? (success * 100.0) / (success + fail) : 0;
        System.out.printf("\nSuccess Rate: %.1f%%%n", successRate);
    }

    /**
     * Generate import report file
     */
    private void generateImportReport(int success, int fail, List<String> errors) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportFile = REPORTS_DIR + "import_report_" + timestamp + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            writer.println("GRADE IMPORT REPORT");
            writer.println("Generated: " + new Date());
            writer.println("=".repeat(60));
            writer.println();

            writer.println("SUMMARY");
            writer.println("-".repeat(40));
            writer.println("Total Records: " + (success + fail));
            writer.println("Successful: " + success);
            writer.println("Failed: " + fail);
            writer.printf("Success Rate: %.1f%%%n",
                    (success + fail) > 0 ? (success * 100.0) / (success + fail) : 0);
            writer.println();

            if (!errors.isEmpty()) {
                writer.println("ERROR DETAILS");
                writer.println("-".repeat(40));
                for (String error : errors) {
                    writer.println("• " + error);
                }
            }

            System.out.println("\n📄 Import report saved to: " + reportFile);

        } catch (IOException e) {
            System.out.println("Failed to save import report: " + e.getMessage());
        }
    }

    /**
     * Export template files for each format
     */
    public void exportImportTemplates() {
        System.out.println("\n📝 EXPORTING IMPORT TEMPLATES");
        System.out.println("=".repeat(50));

        try {
            // CSV Template
            exportCSVTemplate();
            System.out.println("✅ CSV template exported to: " + IMPORTS_DIR + "template.csv");

            // JSON Template
            exportJSONTemplate();
            System.out.println("✅ JSON template exported to: " + IMPORTS_DIR + "template.json");

            // Binary Template
            exportBinaryTemplate();
            System.out.println("✅ Binary template exported to: " + IMPORTS_DIR + "template.bin");

            System.out.println("\n📋 TEMPLATE FORMATS:");
            System.out.println("CSV:   studentId,subjectCode,grade,date(optional),subjectType(optional)");
            System.out.println("JSON:  Array of objects with studentId, subjectCode, grade");
            System.out.println("Binary: Serialized GradeImportRecord objects");

        } catch (Exception e) {
            System.out.println("❌ Failed to export templates: " + e.getMessage());
        }
    }

    private void exportCSVTemplate() throws IOException {
        String csvContent = "# Grade Import Template - CSV Format\n" +
                "# Format: studentId,subjectCode,grade,date(optional),subjectType(optional)\n" +
                "# Example:\n" +
                "STU001,MAT101,85.5,2024-01-15,Core\n" +
                "STU002,ENG101,92.0,2024-01-15,Core\n" +
                "STU001,SCI101,78.5,2024-01-16,Core\n" +
                "STU003,MUS101,88.0,2024-01-16,Elective\n";

        Files.writeString(Paths.get(IMPORTS_DIR + "template.csv"), csvContent);
    }

    private void exportJSONTemplate() throws IOException {
        List<GradeImportRecord> templateRecords = Arrays.asList(
                new GradeImportRecord("STU001", "MAT101", 85.5, "2024-01-15", "Core"),
                new GradeImportRecord("STU002", "ENG101", 92.0, "2024-01-15", "Core"),
                new GradeImportRecord("STU001", "SCI101", 78.5, "2024-01-16", "Core"),
                new GradeImportRecord("STU003", "MUS101", 88.0, "2024-01-16", "Elective")
        );

        String jsonContent = gson.toJson(templateRecords);
        Files.writeString(Paths.get(IMPORTS_DIR + "template.json"), jsonContent);
    }

    private void exportBinaryTemplate() throws IOException {
        List<GradeImportRecord> templateRecords = Arrays.asList(
                new GradeImportRecord("STU001", "MAT101", 85.5, "2024-01-15", "Core"),
                new GradeImportRecord("STU002", "ENG101", 92.0, "2024-01-15", "Core"),
                new GradeImportRecord("STU003", "MUS101", 88.0, "2024-01-16", "Elective")
        );

        BinaryImportWrapper wrapper = new BinaryImportWrapper();
        wrapper.grades = templateRecords;
        wrapper.timestamp = new Date();
        wrapper.description = "Template import file";

        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(Paths.get(IMPORTS_DIR + "template.bin")))) {
            oos.writeObject(wrapper);
        }
    }

    /**
     * List available import files
     */
    public void listImportFiles() {
        System.out.println("\n📁 AVAILABLE IMPORT FILES");
        System.out.println("=".repeat(60));

        File importDir = new File(IMPORTS_DIR);
        if (!importDir.exists() || !importDir.isDirectory()) {
            System.out.println("Import directory not found: " + IMPORTS_DIR);
            return;
        }

        File[] files = importDir.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("No import files found.");
            return;
        }

        System.out.printf("%-30s %-10s %-15s%n", "Filename", "Size", "Format");
        System.out.println("-".repeat(60));

        for (File file : files) {
            if (file.isFile()) {
                String size = formatFileSize(file.length());
                String format = detectFileFormat(file.getName()).toString();
                System.out.printf("%-30s %-10s %-15s%n",
                        file.getName(), size, format);
            }
        }

        System.out.println("\nTotal files: " + files.length);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Shutdown the service
     */
    public void shutdown() {
        if (importExecutor != null) {
            importExecutor.shutdown();
            try {
                if (!importExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    importExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                importExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // ========== Inner Classes ==========

    /**
     * Grade import record structure
     */
    public static class GradeImportRecord {
        public String studentId;
        public String subjectCode;
        public double grade;
        public String date;
        public String subjectType;

        public GradeImportRecord() {}

        public GradeImportRecord(String studentId, String subjectCode, double grade,
                                 String date, String subjectType) {
            this.studentId = studentId;
            this.subjectCode = subjectCode;
            this.grade = grade;
            this.date = date;
            this.subjectType = subjectType;
        }
    }

    /**
     * JSON import wrapper
     */
    private static class JsonImportWrapper {
        public List<GradeImportRecord> grades;
        public Date timestamp;
        public String description;
    }

    /**
     * Binary import wrapper
     */
    private static class BinaryImportWrapper implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<GradeImportRecord> grades;
        public Date timestamp;
        public String description;
    }

    /**
     * Import result
     */
    private static class ImportResult {
        boolean success;
        String errorMessage;

        ImportResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * File format enum
     */
    public enum FileFormat {
        CSV, JSON, BINARY
    }
}