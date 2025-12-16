package org.example.grade;

import org.example.newImprementations.FileExporter;
import org.example.newImprementations.GPACalculator;
import org.example.service.BulkImportService;
import org.example.student.RegularStudent;
import org.example.student.Student;
import org.example.student.StudentManager;
import org.example.subject.CoreSubject;
import org.example.subject.ElectiveSubject;
import org.example.subject.Subject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GradeManager {
    private Grade[] grades = new Grade[200];
    private int gradeCount;
    private StudentManager studentManager;
    private BulkImportService bulkImportService = new BulkImportService();

    Scanner scanner = new Scanner(System.in);

    private Subject[] subject ={
            new CoreSubject("Mathematics", "MAT101"),
            new CoreSubject("English", "EN101"),
            new CoreSubject("Science", "SC101"),
            new ElectiveSubject("Music", "MU101"),
            new ElectiveSubject("Art", "AR101"),
            new ElectiveSubject("Physical Education", "PE101")
    };

    public GradeManager(){
    }

    public GradeManager(StudentManager studentManager){
        this.studentManager = studentManager;
    }

    public void addGrade(Grade theGrade) {
        System.out.println("RECORD GRADE");
        System.out.println("__________________________");
        System.out.print("Enter Student ID: ");
        String id = scanner.nextLine().trim();

        Student student = studentManager.findStudent(id);
        if (student == null) {
            System.out.println("Student with ID " + id + " Not found!");
            return;
        }

        System.out.println("Student Details:");
        System.out.println("Name: " + student.getName());
        System.out.println("Type: " + student.getStudentType());
        System.out.printf("Current Average: %.1f%% " , calculateOverallAverage(id));
        System.out.println();

        System.out.println("Subject Type:");
        System.out.println("1. Core Subject (Mathematics, English, Science)");
        System.out.println("2. Elective Subject (Music, Arts, Physical Education)");
        System.out.print("Select Type (1–2): ");

        int choice;
        while (true) {
            try {
                choice = Integer.parseInt(scanner.nextLine());
                if (choice == 1 || choice == 2) break;
            } catch (Exception ignored) { }
            System.out.print("Invalid! Enter 1 or 2: ");
        }

        Subject[] filtered = new Subject[3];
        int fCount = 0;

        for (Subject s : subject) {
            if (choice == 1 && s.getSubjectType().equals("Core")) {
                filtered[fCount++] = s;
            }
            else if (choice == 2 && s.getSubjectType().equals("Elective")) {
                filtered[fCount++] = s;
            }
        }

        System.out.println(choice == 1 ? "Available Core Subjects:" : "Available Elective Subjects:");
        for (int i = 0; i < fCount; i++) {
            System.out.println((i + 1) + ". " + filtered[i].getSubjectName());
        }

        System.out.print("Select Subject (1–" + fCount + "): ");
        int subjectChoice;

        while (true) {
            try {
                subjectChoice = Integer.parseInt(scanner.nextLine());
                if (subjectChoice >= 1 && subjectChoice <= fCount) break;
            } catch (Exception ignored) { }
            System.out.print("Invalid! Choose between 1–" + fCount + ": ");
        }

        Subject selectedSubject = filtered[subjectChoice - 1];

        System.out.print("Enter grade (0–100): ");
        double gradeValue;

        while (true) {
            try {
                gradeValue = Double.parseDouble(scanner.nextLine());
                if (gradeValue >= 0 && gradeValue <= 100) break;
            } catch (Exception ignored) { }
            System.out.print("Invalid! Enter grade between 0–100: ");
        }

        Grade existing = findExistingGrade(student.getStudentId(), selectedSubject.getSubjectCode());

        if (existing != null) {
            System.out.println("\nA grade for this subject already exists!");
            System.out.println("Current Marks: " + existing.getGrade() + "%");

            System.out.print("Do you want to UPDATE the marks? (Y/N): ");
            String update = scanner.nextLine().trim();

            while (!update.equalsIgnoreCase("Y") && !update.equalsIgnoreCase("N")) {
                System.out.print("Invalid! Enter Y or N: ");
                update = scanner.nextLine().trim();
            }

            if (update.equalsIgnoreCase("Y")) {
                existing.setGrade(gradeValue);
                System.out.println("Grade updated successfully!");

                double newAverage = calculateOverallAverage(id);
                int totalSubjects = getRegisteredSubjects(id);
                FileExporter.updateStudentGradeFile(student, existing, newAverage, totalSubjects);

            } else {
                System.out.println("Update cancelled.");
            }

            System.out.println("Press enter to continue.");
            scanner.nextLine();
            return;
        }

        theGrade = new Grade(student.getStudentId(), selectedSubject, gradeValue);
        theGrade.setGradeId(String.format("GRD%03d", gradeCount + 1));

        System.out.println("\nGRADE CONFIRMATION");
        System.out.println("_________________________________");
        System.out.println("Grade ID: " + theGrade.getGradeId());
        System.out.println("Student: " + student.getStudentId() + " - " + student.getName());
        System.out.println("Subject: " + selectedSubject.getSubjectName());
        System.out.println("Grade: " + gradeValue + "%");
        System.out.println("Date: " + theGrade.getDate());
        System.out.print("Confirm grade? (Y/N): ");

        String confirm = scanner.nextLine().trim();

        while (!confirm.equalsIgnoreCase("Y") && !confirm.equalsIgnoreCase("N")) {
            System.out.print("Invalid! Please enter Y or N: ");
            confirm = scanner.nextLine().trim();
        }

        if (confirm.equalsIgnoreCase("Y")) {
            if (gradeCount >= grades.length) {
                System.out.println("Cannot add more grades. Storage is full.");
                return;
            }
            grades[gradeCount++] = theGrade;
            System.out.println("Grade saved successfully!");

            double newAverage = calculateOverallAverage(id);
            int totalSubjects = getRegisteredSubjects(id);
            FileExporter.updateStudentGradeFile(student, theGrade, newAverage, totalSubjects);
            bulkImportService.saveOrUpdateGradeCSV(student, theGrade);

            System.out.println("Press enter to continue.");
            scanner.nextLine();
        } else {
            System.out.println("Grade recording cancelled.");
            System.out.println("Press enter to continue.");
            scanner.nextLine();
        }
    }

    public void viewGradeByStudent(String studentId){
        System.out.println("VIEW GRADE");
        System.out.println("__________________________");
        System.out.print("Enter Student ID: ");
        String id = scanner.nextLine().trim();

        Student student = studentManager.findStudent(id);
        if (student == null) {
            System.out.println("Student with ID " + id + " Not found!");
            return;
        }

        int foundCount = 0;
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null && g.getStudentId().equals(id)) {
                foundCount++;
            }
        }

        if (foundCount == 0) {
            System.out.println("Student: " + student.getStudentId() + " - " + student.getName());
            System.out.println("Type: " + student.getStudentType());

            double passingRequirement = (student instanceof RegularStudent) ? 50 : 60;
            System.out.println("Passing: " + (int) passingRequirement + "%");
            System.out.println();
            System.out.println("----------------------------------------------------------");
            System.out.println("No grades recorded for this student.");
            System.out.println("----------------------------------------------------------");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        System.out.println("Student: " + student.getStudentId() + " - " + student.getName());
        System.out.println("Type: " + student.getStudentType() + " Student");

        double passingRequirement = (student instanceof RegularStudent) ? 50 : 60;
        System.out.printf("Current Average: %.1f%%", calculateOverallAverage(id));
        if(calculateOverallAverage(id) < passingRequirement){
            System.out.println("\nStatus: "  + "Failing ❌" );
        } else {
            System.out.println("\nStatus: " + "Passing ✅");
        }

        System.out.println("\nGRADE HISTORY");
        System.out.println("_________________________________________________________________________________________________");
        System.out.printf("%-15s | %-20s | %-12s | %-12s | %-10s %n", "GRD ID","DATE", "SUBJECT", "TYPE", "GRADE");
        System.out.println("-------------------------------------------------------------------------------------------------");

        int count = 0;
        for (int i = gradeCount - 1; i >= 0; i--) {
            Grade g = grades[i];
            if (g != null && g.getStudentId().equals(id)) {
                g.displayGradeDetails();
                count++;
            }
        }

        System.out.println("-------------------------------------------------------------------------------------------------");
        System.out.println("Total Grades: " + count);
        double coreAvg = calculateCoreAverage(id);
        double electiveAvg = calculateElectiveAverage(id);
        double overallAvg = calculateOverallAverage(id);

        System.out.printf("Core subjects Average: %.1f%%\n", coreAvg);
        System.out.printf("Elective Subjects Average: %.1f%%\n", electiveAvg);
        System.out.printf("Overall Average: %.1f%%\n", overallAvg);

        System.out.println();
        System.out.println("Performance Summary: ");

        if (coreAvg >= passingRequirement) {
            System.out.println("✅ Passing all core subjects (Required: " + (int) passingRequirement + "%)");
        } else {
            System.out.println("❌ Not passing core subjects (Required: " + (int) passingRequirement + "%)");
        }

        if (overallAvg >= passingRequirement) {
            System.out.println("✅ Meeting overall passing requirement (" + (int) passingRequirement + "%)");
        } else {
            System.out.println("❌ Not meeting overall passing requirement (" + (int) passingRequirement + "%)");
        }

        System.out.println("----------------------------------------------------------");
        System.out.print("Export this report to file? (Y/N): ");
        String exportChoice = scanner.nextLine().trim().toUpperCase();

        if (exportChoice.equals("Y")) {
            exportStudentGradeReport(id);
        }

        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    public double calculateCoreAverage(String studentId){
        int countCore = 0;
        int coreTotal = 0;
        for(int i = 0; i < gradeCount; i++){
            if(grades[i] != null && grades[i].getStudentId().equals(studentId) && grades[i].getSubject().getSubjectType().equals("Core")){
                coreTotal += grades[i].getGrade();
                countCore++;
            }
        }

        double averageCore = (countCore > 0) ? (double) coreTotal / countCore : 0;
        return averageCore;
    }

    public double calculateElectiveAverage(String studentId){
        int electiveCount = 0;
        int electiveTotal = 0;

        for(int i = 0; i < gradeCount; i++){
            if(grades[i] != null && grades[i].getStudentId().equals(studentId) && grades[i].getSubject().getSubjectType().equals("Elective")){
                electiveTotal += grades[i].getGrade();
                electiveCount++;
            }
        }

        double averageElective = (electiveCount > 0) ? (double) electiveTotal / electiveCount : 0;
        return averageElective;
    }

    public double calculateOverallAverage(String studentId){
        int totalGrades = 0;
        int countGrades = 0;
        for(int i = 0; i < gradeCount; i++){
            if(grades[i] != null && grades[i].getStudentId().equals(studentId)){
                totalGrades += grades[i].getGrade();
                countGrades ++;
            }
        }

        double overallAverage = (countGrades > 0) ? (double) totalGrades/ countGrades : 0;
        return overallAverage;
    }

    public int getGradeCount(){
        return gradeCount;
    }

    public int getRegisteredSubjects(String studentId) {
        String[] subjectCodes = new String[50];
        int count = 0;

        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null && g.getStudentId().equals(studentId)) {
                String code = g.getSubject().getSubjectCode();

                boolean exists = false;
                for (int j = 0; j < count; j++) {
                    if (subjectCodes[j].equals(code)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    subjectCodes[count++] = code;
                }
            }
        }

        return count;
    }

    private Grade findExistingGrade(String studentId, String subjectCode) {
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null &&
                    g.getStudentId().equals(studentId) &&
                    g.getSubject().getSubjectCode().equals(subjectCode)) {
                return g;
            }
        }
        return null;
    }

    public void exportGradeReport() {
        System.out.println("EXPORT GRADE REPORT");
        System.out.println("__________________________");

        System.out.print("Enter Student ID: ");
        String id = scanner.nextLine().trim();

        Student student = studentManager.findStudent(id);
        if (student == null) {
            System.out.println("Student with ID " + id + " not found!");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        System.out.println("\nStudent Details:");
        System.out.println("ID: " + student.getStudentId());
        System.out.println("Name: " + student.getName());
        System.out.println("Type: " + student.getStudentType());

        List<Grade> studentGrades = new ArrayList<>();
        int totalGrades = 0;
        double totalScore = 0;

        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null && g.getStudentId().equals(id)) {
                studentGrades.add(g);
                totalGrades++;
                totalScore += g.getGrade();
            }
        }

        if (studentGrades.isEmpty()) {
            System.out.println("No grades found for this student.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        double average = totalScore / totalGrades;

        System.out.println("Export options:");
        System.out.println("1. Summary Report (Overview Only)");
        System.out.println("2. Detailed Report (All grades)");
        System.out.println("3. Both");

        System.out.print("\nEnter choice (1-3): ");
        int choice;
        while (true) {
            try {
                choice = Integer.parseInt(scanner.nextLine());
                if (choice >= 1 && choice <= 3) break;
                else System.out.print("Invalid! Enter 1, 2, or 3: ");
            } catch (Exception ignored) {
                System.out.print("Invalid! Enter a number (1-3): ");
            }
        }

        System.out.print("\nEnter file name without extension: ");
        String baseFileName = scanner.nextLine().trim();

        String studentFileName = student.getName().toLowerCase()
                .replace(" ", "_")
                .replaceAll("[^a-z_]", "") + "_report";

        List<File> exportedFiles = new ArrayList<>();

        if (choice == 1 || choice == 3) {
            File summaryFile = FileExporter.exportStudentToFile(
                    student,
                    average,
                    studentGrades.size(),
                    baseFileName + "_summary",
                    false
            );
            if (summaryFile != null) {
                exportedFiles.add(summaryFile);
            }
        }

        if (choice == 2 || choice == 3) {
            File detailedFile = FileExporter.exportStudentToFile(
                    student,
                    average,
                    studentGrades.size(),
                    baseFileName + "_detailed",
                    true
            );
            if (detailedFile != null) {
                exportedFiles.add(detailedFile);
            }
        }

        if (!exportedFiles.isEmpty()) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("EXPORT COMPLETED SUCCESSFULLY!");
            System.out.println("=".repeat(60));

            for (File file : exportedFiles) {
                System.out.println("\n📁 File Information:");
                System.out.println("   File: " + file.getName());
                System.out.println("   Size: " + file.length() + " bytes");
                System.out.println("   Location: " + file.getAbsolutePath());

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    System.out.println("   Contains:");
                    String line;
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null && lineCount < 8) {
                        if (line.trim().isEmpty()) continue;
                        System.out.println("     • " + line.trim());
                        lineCount++;
                    }
                    if (lineCount == 8) {
                        System.out.println("     • ... (more content truncated)");
                    }
                } catch (IOException e) {
                    System.out.println("   Could not read file contents for preview.");
                }
            }

            File studentSpecificFile = FileExporter.exportStudentToFile(
                    student,
                    average,
                    studentGrades.size(),
                    studentFileName,
                    true
            );

            if (studentSpecificFile != null) {
                System.out.println("\n📝 Also saved to student-specific file:");
                System.out.println("   File: " + studentSpecificFile.getName());
                System.out.println("   This file will be updated with future grades.");
            }
        } else {
            System.out.println("No files were exported.");
        }

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    public void exportAllGradesToCSV() {
        System.out.println("EXPORT ALL GRADES TO CSV");
        System.out.println("__________________________");

        if (gradeCount == 0) {
            System.out.println("No grades available to export!");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        Student[] students = studentManager.getAllStudents();
        int studentCount = studentManager.getStudentCount();

        FileExporter.exportAllGradesToCSV(grades, gradeCount, students, studentCount);

        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    public void exportStudentGradeReport(String studentId) {
        Student student = studentManager.findStudent(studentId);
        if (student == null) {
            System.out.println("Student not found!");
            return;
        }

        double averageGrade = calculateOverallAverage(studentId);
        int subjectCount = getRegisteredSubjects(studentId);

        String studentFileName = student.getName().toLowerCase()
                .replace(" ", "_")
                .replaceAll("[^a-z_]", "") + "_report";

        File exported = FileExporter.exportStudentToFile(
                student,
                averageGrade,
                subjectCount,
                studentFileName,
                true
        );

        if (exported != null) {
            System.out.println("Student report exported: " + exported.getAbsolutePath());
        } else {
            System.out.println("Failed to export student report.");
        }
    }

    public void calculateStudentGPA() {
        System.out.println("\nCALCULATE STUDENT GPA");
        System.out.println("__________________________");
        System.out.print("Enter Student ID: ");
        String id = scanner.nextLine();

        Student student = studentManager.findStudent(id);
        if (student == null) {
            System.out.println("Student not found!");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        List<Grade> grades = getGradesByStudentId(id);
        if (grades.isEmpty()) {
            System.out.println("This student has no recorded grades.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }


        System.out.println("\n=============================================");
        System.out.println("   GPA REPORT FOR " + student.getStudentId());
        System.out.println("=============================================");
        System.out.println("Student: " + student.getStudentId() + " - " + student.getName());
        System.out.println("Type: " + student.getStudentType());
        System.out.println("Overall Average: " + String.format("%.2f", calculateOverallAverage(id)) + "%");
        System.out.println();

        System.out.println("GPA Calculation (4.0 Scale)");
        System.out.println("------------------------------------------------------------");
        System.out.printf("%-25s %-12s %-10s %-10s\n", "COURSE", "PERCENTAGE", "GPA", "LETTER");
        System.out.println("------------------------------------------------------------");

        double totalGPA = 0;
        for (Grade g : grades) {
            double percentage = g.getGrade();
            double courseGPA = GPACalculator.percentageToGPA(percentage);
            String letter = GPACalculator.gpaToLetter(courseGPA);
            totalGPA += courseGPA;

            System.out.printf("%-25s %-12.2f %-10.2f %-10s\n",
                    g.getSubject().getSubjectName(),
                    percentage,
                    courseGPA,
                    letter);
        }

        System.out.println("----------------------------------------------------------------");

        double cumulativeGPA = totalGPA / grades.size();
        String cumulativeLetter = GPACalculator.gpaToLetter(cumulativeGPA);

        int rank = calculateClassRank(id);

        double classAverage = studentManager.getAverageClassGrade();
        double classGPA = GPACalculator.percentageToGPA(classAverage);

        System.out.println("\nCumulative GPA: " + String.format("%.2f", cumulativeGPA) + " / 4.0");
        System.out.println("Letter Grade: " + cumulativeLetter);
        if (rank > 0) {
            System.out.println("Class Rank: " + rank + " of " + studentManager.getStudentCount());
        }
        System.out.println();

        System.out.println("Performance Analysis:");

        if (student.getStudentType().equals("Honors")) {
            if (cumulativeGPA >= 3.5) {
                System.out.println("✓ Honors Eligibility: MAINTAINED");
            } else {
                System.out.println("⚠ Honors Eligibility: AT RISK");
                System.out.println("  Current GPA: " + String.format("%.2f", cumulativeGPA) + " | Required: 3.50");
            }
        }

        if (cumulativeGPA >= 3.7) {
            System.out.println("✓ Excellent Performance" + String.format("%.2f", cumulativeGPA) + " GPA");
        } else if (cumulativeGPA >= 3.3) {
            System.out.println("✓ Very Good Performance" + String.format("%.2f", cumulativeGPA) + " GPA");
        } else if (cumulativeGPA >= 3.0) {
            System.out.println("✓ Good Performance" + String.format("%.2f", cumulativeGPA) + " GPA");
        } else if (cumulativeGPA >= 2.0) {
            System.out.println("✓ Satisfactory Performance" + String.format("%.2f", cumulativeGPA) + " GPA");
        } else {
            System.out.println("⚠ Needs Improvement" + String.format("%.2f", cumulativeGPA) + " GPA");
        }

        if (cumulativeGPA > classGPA + 0.5) {
            System.out.println("✓ Significantly Above Class Average" + String.format("%.2f", cumulativeGPA) + " GPA");
        } else if (cumulativeGPA > classGPA + 0.2) {
            System.out.println("✓ Above Class Average" + String.format("%.2f", cumulativeGPA) + " GPA");
        } else if (cumulativeGPA >= classGPA - 0.2) {
            System.out.println("✓ At Class Average Level" + String.format("%.2f", cumulativeGPA) + " GPA");
        } else {
            System.out.println("⚠ Below Class Average" + String.format("%.2f", cumulativeGPA) + " GPA");
        }
        System.out.println("  Your GPA: " + String.format("%.2f", cumulativeGPA) +
                " | Class Avg GPA: " + String.format("%.2f", classGPA));

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }


    private List<Grade> getGradesByStudentId(String studentId) {
        List<Grade> studentGrades = new ArrayList<>();
        for (int i = 0; i < gradeCount; i++) {
            Grade g = grades[i];
            if (g != null && g.getStudentId().equals(studentId)) {
                studentGrades.add(g);
            }
        }
        return studentGrades;
    }


    private int calculateClassRank(String studentId) {
        Student[] students = studentManager.getAllStudents();
        int totalStudents = studentManager.getStudentCount();

        List<StudentAverage> studentAverages = new ArrayList<>();
        for (int i = 0; i < totalStudents; i++) {
            Student s = students[i];
            if (s != null) {
                double avg = calculateOverallAverage(s.getStudentId());
                studentAverages.add(new StudentAverage(s.getStudentId(), avg));
            }
        }

        studentAverages.sort((a, b) -> Double.compare(b.average, a.average));

        for (int i = 0; i < studentAverages.size(); i++) {
            if (studentAverages.get(i).studentId.equals(studentId)) {
                return i + 1;
            }
        }
        return -1;
    }


    private static class StudentAverage {
        String studentId;
        double average;

        StudentAverage(String studentId, double average) {
            this.studentId = studentId;
            this.average = average;
        }
    }

    public void bulkImportGrades() {
        System.out.println("BULK IMPORT GRADES");
        System.out.println("---------------------------------------");

        String importDir = "./imports/";
        System.out.println("Place your CSV file in: " + importDir);
        System.out.println();
        System.out.println("CSV Format Required:");
        System.out.println("StudentID,SubjectName,SubjectType,Grade");
        System.out.println("Example: STU001,Mathematics,Core,85");
        System.out.println();

        System.out.print("Enter filename (without extension): ");
        String filename = scanner.nextLine().trim();

        File file = new File(importDir + filename + ".csv");

        if (!file.exists()) {
            System.out.println("❌ File not found: " + file.getAbsolutePath());
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        System.out.println("\nValidating file... ✓");
        System.out.println("Processing grades...\n");

        int totalRows = 0;
        int successCount = 0;
        int failCount = 0;

        List<String> failedRows = new ArrayList<>();
        List<String> logLines = new ArrayList<>();

        String logFileName = "import_log_" +
                java.time.LocalDate.now().toString().replace("-", "") + ".txt";

        File logFile = new File(logFileName);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                totalRows++;

                String[] parts = line.split(",");
                if (parts.length != 4) {
                    failCount++;
                    failedRows.add("Row " + totalRows + ": Invalid format");
                    logLines.add("Row " + totalRows + " FAILED - Invalid format");
                    continue;
                }

                String studentId = parts[0].trim();
                String subjectName = parts[1].trim();
                String subjectType = parts[2].trim();
                String gradeStr = parts[3].trim();

                Student student = studentManager.findStudent(studentId);
                if (student == null) {
                    failCount++;
                    failedRows.add("Row " + totalRows + ": Invalid student ID (" + studentId + ")");
                    logLines.add("Row " + totalRows + " FAILED - Student not found: " + studentId);
                    continue;
                }

                Subject selectedSubject = null;
                for (Subject s : subject) {
                    if (s.getSubjectName().equalsIgnoreCase(subjectName)
                            && s.getSubjectType().equalsIgnoreCase(subjectType)) {
                        selectedSubject = s;
                        break;
                    }
                }

                if (selectedSubject == null) {
                    failCount++;
                    failedRows.add("Row " + totalRows + ": Invalid subject or type (" + subjectName + ")");
                    logLines.add("Row " + totalRows + " FAILED - Subject invalid: " + subjectName);
                    continue;
                }

                double gradeValue;
                try {
                    gradeValue = Double.parseDouble(gradeStr);
                    if (gradeValue < 0 || gradeValue > 100) {
                        failCount++;
                        failedRows.add("Row " + totalRows + ": Grade out of range (" + gradeStr + ")");
                        logLines.add("Row " + totalRows + " FAILED - Grade out of range: " + gradeStr);
                        continue;
                    }
                } catch (Exception e) {
                    failCount++;
                    failedRows.add("Row " + totalRows + ": Invalid grade (" + gradeStr + ")");
                    logLines.add("Row " + totalRows + " FAILED - Invalid grade: " + gradeStr);
                    continue;
                }

                Grade existing = findExistingGrade(studentId, selectedSubject.getSubjectCode());

                if (existing != null) {
                    existing.setGrade(gradeValue);
                    bulkImportService.saveOrUpdateGradeCSV(student, existing);
                    logLines.add("Row " + totalRows + " UPDATED - " + studentId + " " + subjectName);
                } else {
                    Grade g = new Grade(studentId, selectedSubject, gradeValue);
                    g.setGradeId(String.format("GRD%03d", gradeCount + 1));
                    grades[gradeCount++] = g;
                    bulkImportService.saveOrUpdateGradeCSV(student, g);
                    logLines.add("Row " + totalRows + " SUCCESS - " + studentId + " " + subjectName);
                }

                successCount++;
            }

            java.nio.file.Files.write(logFile.toPath(), logLines);

        } catch (IOException e) {
            System.out.println("❌ Error reading file: " + e.getMessage());
            return;
        }

        System.out.println("IMPORT SUMMARY");
        System.out.println("---------------------------------------");
        System.out.println("Total Rows: " + totalRows);
        System.out.println("Successfully Imported: " + successCount);
        System.out.println("Failed: " + failCount);

        if (!failedRows.isEmpty()) {
            System.out.println("\nFailed Records:");
            for (String err : failedRows) {
                System.out.println(err);
            }
        }

        System.out.println("\n✓ Import completed!");
        System.out.println("   " + successCount + " grades added to system");
        System.out.println("   See " + logFileName + " for details");

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    public void viewClassStatistics() {
        System.out.println("\n=============================================");
        System.out.println("        CLASS STATISTICS - OVERVIEW");
        System.out.println("=============================================");

        int totalStudents = studentManager.getStudentCount();

        if (totalStudents == 0) {
            System.out.println("No students in the system.");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        // SECTION 1: BASIC OVERVIEW
        System.out.println("\n1. BASIC OVERVIEW");
        System.out.println("   " + "-".repeat(40));
        System.out.printf("   Total Students: %d%n", totalStudents);
        System.out.printf("   Total Grades Recorded: %d%n", gradeCount);

        // Count regular vs honors students
        int regularCount = 0;
        int honorsCount = 0;
        Student[] allStudents = studentManager.getAllStudents();

        for (int i = 0; i < totalStudents; i++) {
            if (allStudents[i] != null) {
                if (allStudents[i].getStudentType().equals("Regular")) {
                    regularCount++;
                } else if (allStudents[i].getStudentType().equals("Honors")) {
                    honorsCount++;
                }
            }
        }

        System.out.printf("   Regular Students: %d (%.1f%%)%n",
                regularCount,
                totalStudents > 0 ? (regularCount * 100.0 / totalStudents) : 0);
        System.out.printf("   Honors Students: %d (%.1f%%)%n",
                honorsCount,
                totalStudents > 0 ? (honorsCount * 100.0 / totalStudents) : 0);

        // SECTION 2: GRADE DISTRIBUTION
        System.out.println("\n2. GRADE DISTRIBUTION");
        System.out.println("   " + "-".repeat(40));

        if (gradeCount == 0) {
            System.out.println("   No grades recorded yet.");
        } else {
            int[] gradeRanges = new int[5]; // 0: 90-100, 1: 80-89, 2: 70-79, 3: 60-69, 4: 0-59
            String[] rangeLabels = {
                    "90% - 100% (Excellent)",
                    "80% - 89%  (Very Good)",
                    "70% - 79%  (Good)",
                    "60% - 69%  (Satisfactory)",
                    "0%  - 59%  (Needs Improvement)"
            };

            for (int i = 0; i < gradeCount; i++) {
                if (grades[i] != null) {
                    double grade = grades[i].getGrade();
                    if (grade >= 90) gradeRanges[0]++;
                    else if (grade >= 80) gradeRanges[1]++;
                    else if (grade >= 70) gradeRanges[2]++;
                    else if (grade >= 60) gradeRanges[3]++;
                    else gradeRanges[4]++;
                }
            }

            for (int i = 0; i < gradeRanges.length; i++) {
                double percentage = (gradeRanges[i] * 100.0) / gradeCount;
                System.out.printf("   %-30s : %3d grades (%.1f%%)%n",
                        rangeLabels[i], gradeRanges[i], percentage);
            }
        }

        // SECTION 3: STATISTICAL ANALYSIS
        System.out.println("\n3. STATISTICAL ANALYSIS");
        System.out.println("   " + "-".repeat(40));

        if (gradeCount > 0) {
            // Collect all grades for calculations
            double[] allGrades = new double[gradeCount];
            int validGradeCount = 0;
            for (int i = 0; i < gradeCount; i++) {
                if (grades[i] != null) {
                    allGrades[validGradeCount++] = grades[i].getGrade();
                }
            }

            // Calculate mean
            double sum = 0;
            double min = 100;
            double max = 0;
            for (int i = 0; i < validGradeCount; i++) {
                double grade = allGrades[i];
                sum += grade;
                if (grade < min) min = grade;
                if (grade > max) max = grade;
            }
            double mean = sum / validGradeCount;

            // Sort for median calculation
            double[] sortedGrades = new double[validGradeCount];
            System.arraycopy(allGrades, 0, sortedGrades, 0, validGradeCount);
            java.util.Arrays.sort(sortedGrades);

            // Calculate median
            double median;
            if (validGradeCount % 2 == 0) {
                median = (sortedGrades[validGradeCount/2 - 1] + sortedGrades[validGradeCount/2]) / 2.0;
            } else {
                median = sortedGrades[validGradeCount/2];
            }

            // Calculate mode
            double mode = calculateMode(sortedGrades);

            // Calculate range
            double range = max - min;

            // Calculate standard deviation
            double variance = 0;
            for (int i = 0; i < validGradeCount; i++) {
                variance += Math.pow(allGrades[i] - mean, 2);
            }
            variance /= validGradeCount;
            double stdDev = Math.sqrt(variance);

            // Find highest and lowest grade details
            Grade highestGrade = null;
            Grade lowestGrade = null;
            for (int i = 0; i < gradeCount; i++) {
                if (grades[i] != null) {
                    if (highestGrade == null || grades[i].getGrade() > highestGrade.getGrade()) {
                        highestGrade = grades[i];
                    }
                    if (lowestGrade == null || grades[i].getGrade() < lowestGrade.getGrade()) {
                        lowestGrade = grades[i];
                    }
                }
            }

            System.out.printf("   Mean (Average): %.2f%%%n", mean);
            System.out.printf("   Median: %.2f%%%n", median);
            System.out.printf("   Mode: %.2f%%%n", mode);
            System.out.printf("   Range: %.2f%% (%.1f to %.1f)%n", range, min, max);
            System.out.printf("   Standard Deviation: %.2f%%%n", stdDev);

            if (highestGrade != null) {
                Student highestStudent = studentManager.findStudent(highestGrade.getStudentId());
                System.out.printf("   Highest Grade: %.1f%%%n", highestGrade.getGrade());
                System.out.printf("     Student: %s (%s)%n",
                        highestStudent != null ? highestStudent.getName() : "Unknown",
                        highestGrade.getStudentId());
                System.out.printf("     Course: %s (%s)%n",
                        highestGrade.getSubject().getSubjectName(),
                        highestGrade.getSubject().getSubjectCode());
            }

            if (lowestGrade != null) {
                Student lowestStudent = studentManager.findStudent(lowestGrade.getStudentId());
                System.out.printf("   Lowest Grade: %.1f%%%n", lowestGrade.getGrade());
                System.out.printf("     Student: %s (%s)%n",
                        lowestStudent != null ? lowestStudent.getName() : "Unknown",
                        lowestGrade.getStudentId());
                System.out.printf("     Course: %s (%s)%n",
                        lowestGrade.getSubject().getSubjectName(),
                        lowestGrade.getSubject().getSubjectCode());
            }
        } else {
            System.out.println("   No grades available for statistical analysis.");
        }

        // SECTION 4: SUBJECT PERFORMANCE
        System.out.println("\n4. SUBJECT PERFORMANCE");
        System.out.println("   " + "-".repeat(40));

        // Core subjects analysis
        System.out.println("\n   CORE SUBJECTS:");
        System.out.println("   " + "-".repeat(35));

        double coreTotal = 0;
        int coreSubjectCount = 0;
        int totalCoreGrades = 0;

        for (Subject subj : subject) {
            if (subj != null && subj.getSubjectType().equals("Core")) {
                double subjectTotal = 0;
                int subjectCount = 0;

                for (int i = 0; i < gradeCount; i++) {
                    if (grades[i] != null &&
                            grades[i].getSubject().getSubjectCode().equals(subj.getSubjectCode())) {
                        subjectTotal += grades[i].getGrade();
                        subjectCount++;
                        totalCoreGrades++;
                    }
                }

                if (subjectCount > 0) {
                    double subjectAvg = subjectTotal / subjectCount;
                    coreTotal += subjectAvg;
                    coreSubjectCount++;
                    System.out.printf("   %-20s: %6.1f%% (%d grades)%n",
                            subj.getSubjectName(), subjectAvg, subjectCount);
                } else {
                    System.out.printf("   %-20s: No grades recorded%n", subj.getSubjectName());
                }
            }
        }

        if (coreSubjectCount > 0) {
            System.out.println("   " + "-".repeat(35));
            System.out.printf("   Overall Core Average: %.1f%% (%d total grades)%n",
                    coreTotal / coreSubjectCount, totalCoreGrades);
        }

        // Elective subjects analysis
        System.out.println("\n   ELECTIVE SUBJECTS:");
        System.out.println("   " + "-".repeat(35));

        double electiveTotal = 0;
        int electiveSubjectCount = 0;
        int totalElectiveGrades = 0;

        for (Subject subj : subject) {
            if (subj != null && subj.getSubjectType().equals("Elective")) {
                double subjectTotal = 0;
                int subjectCount = 0;

                for (int i = 0; i < gradeCount; i++) {
                    if (grades[i] != null &&
                            grades[i].getSubject().getSubjectCode().equals(subj.getSubjectCode())) {
                        subjectTotal += grades[i].getGrade();
                        subjectCount++;
                        totalElectiveGrades++;
                    }
                }

                if (subjectCount > 0) {
                    double subjectAvg = subjectTotal / subjectCount;
                    electiveTotal += subjectAvg;
                    electiveSubjectCount++;
                    System.out.printf("   %-20s: %6.1f%% (%d grades)%n",
                            subj.getSubjectName(), subjectAvg, subjectCount);
                } else {
                    System.out.printf("   %-20s: No grades recorded%n", subj.getSubjectName());
                }
            }
        }

        if (electiveSubjectCount > 0) {
            System.out.println("   " + "-".repeat(35));
            System.out.printf("   Overall Elective Average: %.1f%% (%d total grades)%n",
                    electiveTotal / electiveSubjectCount, totalElectiveGrades);
        }

        // SECTION 5: PASSING RATES
        System.out.println("\n5. PASSING RATES ANALYSIS");
        System.out.println("   " + "-".repeat(40));

        if (gradeCount > 0) {
            int passingCount = 0;
            int failingCount = 0;
            int honorsPassing = 0;
            int regularPassing = 0;

            for (int i = 0; i < gradeCount; i++) {
                if (grades[i] != null) {
                    Student student = studentManager.findStudent(grades[i].getStudentId());
                    if (student != null) {
                        if (grades[i].getGrade() >= student.getPassingGrade()) {
                            passingCount++;
                            if (student.getStudentType().equals("Honors")) {
                                honorsPassing++;
                            } else {
                                regularPassing++;
                            }
                        } else {
                            failingCount++;
                        }
                    }
                }
            }

            double passingRate = (passingCount * 100.0) / gradeCount;
            double failingRate = (failingCount * 100.0) / gradeCount;

            System.out.printf("   Overall Passing Rate: %.1f%% (%d/%d)%n",
                    passingRate, passingCount, gradeCount);
            System.out.printf("   Overall Failing Rate: %.1f%% (%d/%d)%n",
                    failingRate, failingCount, gradeCount);

            // Calculate class average using student averages
            double classTotalAvg = 0;
            int studentsWithGrades = 0;

            for (int i = 0; i < totalStudents; i++) {
                if (allStudents[i] != null) {
                    double studentAvg = calculateOverallAverage(allStudents[i].getStudentId());
                    if (studentAvg > 0) {
                        classTotalAvg += studentAvg;
                        studentsWithGrades++;
                    }
                }
            }

            if (studentsWithGrades > 0) {
                System.out.printf("   Class Average (per student): %.1f%%%n",
                        classTotalAvg / studentsWithGrades);
            } else {
                // If we can't access students array, use a simpler calculation
                double totalGradeSum = 0;
                for (int i = 0; i < gradeCount; i++) {
                    if (grades[i] != null) {
                        totalGradeSum += grades[i].getGrade();
                    }
                }
                if (gradeCount > 0) {
                    System.out.printf("   Class Average (per grade): %.1f%%%n",
                            totalGradeSum / gradeCount);
                }
            }

            // Show passing rates by student type if we have data
            if (honorsPassing + regularPassing > 0) {
                int honorsTotalGrades = 0;
                int regularTotalGrades = 0;

                for (int i = 0; i < gradeCount; i++) {
                    if (grades[i] != null) {
                        Student student = studentManager.findStudent(grades[i].getStudentId());
                        if (student != null) {
                            if (student.getStudentType().equals("Honors")) {
                                honorsTotalGrades++;
                            } else {
                                regularTotalGrades++;
                            }
                        }
                    }
                }

                if (honorsTotalGrades > 0) {
                    double honorsPassRate = (honorsPassing * 100.0) / honorsTotalGrades;
                    System.out.printf("   Honors Students Passing: %.1f%% (%d/%d)%n",
                            honorsPassRate, honorsPassing, honorsTotalGrades);
                }

                if (regularTotalGrades > 0) {
                    double regularPassRate = (regularPassing * 100.0) / regularTotalGrades;
                    System.out.printf("   Regular Students Passing: %.1f%% (%d/%d)%n",
                            regularPassRate, regularPassing, regularTotalGrades);
                }
            }
        } else {
            System.out.println("   No grades available for passing rate analysis.");
        }

        // SECTION 6: PERFORMANCE SUMMARY
        System.out.println("\n6. PERFORMANCE SUMMARY");
        System.out.println("   " + "-".repeat(40));

        if (gradeCount > 0) {
            // Count excellent performers (90%+)
            int excellentCount = 0;
            int failingCount = 0;

            for (int i = 0; i < gradeCount; i++) {
                if (grades[i] != null) {
                    if (grades[i].getGrade() >= 90) excellentCount++;
                    if (grades[i].getGrade() < 60) failingCount++;
                }
            }

            System.out.printf("   Excellent Performers (90%%+): %d (%.1f%%)%n",
                    excellentCount, (excellentCount * 100.0) / gradeCount);
            System.out.printf("   Students Needing Help (<60%%): %d (%.1f%%)%n",
                    failingCount, (failingCount * 100.0) / gradeCount);

            // Calculate grade distribution quality
            double averageGrade = 0;
            for (int i = 0; i < gradeCount; i++) {
                if (grades[i] != null) {
                    averageGrade += grades[i].getGrade();
                }
            }
            averageGrade /= gradeCount;

            System.out.println("\n   Overall Performance Level:");
            if (averageGrade >= 85) {
                System.out.println("   ✅ EXCELLENT - Class is performing very well");
            } else if (averageGrade >= 75) {
                System.out.println("   👍 GOOD - Class performance is satisfactory");
            } else if (averageGrade >= 65) {
                System.out.println("   ⚠ AVERAGE - Some improvement needed");
            } else {
                System.out.println("   ❌ NEEDS IMPROVEMENT - Significant attention required");
            }
            System.out.printf("   Average Grade: %.1f%%%n", averageGrade);
        }

        System.out.println("\n" + "=".repeat(55));
        System.out.println("End of Class Statistics Report");
        System.out.println("=".repeat(55));

        System.out.println("\nOptions:");
        System.out.println("1. Export this report to file");
        System.out.println("2. Return to main menu");
        System.out.print("Enter choice (1-2): ");

        try {
            int choice = Integer.parseInt(scanner.nextLine());
            if (choice == 1) {
                exportStatisticsReport();
            }
        } catch (Exception e) {
            // Just continue
        }

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // Helper method to calculate mode
    private double calculateMode(double[] numbers) {
        if (numbers.length == 0) return 0;

        // Group by rounded values (to nearest integer)
        java.util.Map<Integer, Integer> frequencyMap = new java.util.HashMap<>();
        for (double num : numbers) {
            int rounded = (int) Math.round(num);
            frequencyMap.put(rounded, frequencyMap.getOrDefault(rounded, 0) + 1);
        }

        // Find the value with highest frequency
        int maxFrequency = 0;
        int modeValue = 0;
        for (java.util.Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxFrequency) {
                maxFrequency = entry.getValue();
                modeValue = entry.getKey();
            }
        }

        return modeValue;
    }

    // Method to export the statistics report
    private void exportStatisticsReport() {
        System.out.println("\nExporting class statistics report...");

        String reportsDir = "./reports";
        java.io.File directory = new java.io.File(reportsDir);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                System.out.println("❌ Failed to create reports directory!");
                return;
            }
        }

        String fileName = reportsDir + "/class_statistics_" +
                java.time.LocalDate.now().toString().replace("-", "") + ".txt";

        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileName))) {
            writer.println("=".repeat(60));
            writer.println("CLASS STATISTICS REPORT");
            writer.println("Generated on: " + java.time.LocalDateTime.now());
            writer.println("=".repeat(60));

            // Get total students
            int totalStudents = studentManager.getStudentCount();

            writer.println("\n1. BASIC OVERVIEW");
            writer.println("-".repeat(40));
            writer.printf("Total Students: %d%n", totalStudents);
            writer.printf("Total Grades Recorded: %d%n", gradeCount);

            // Get student type breakdown
            int regularCount = 0;
            int honorsCount = 0;
            Student[] students = studentManager.getAllStudents();

            for (int i = 0; i < totalStudents; i++) {
                if (students[i] != null) {
                    if (students[i].getStudentType().equals("Regular")) regularCount++;
                    else if (students[i].getStudentType().equals("Honors")) honorsCount++;
                }
            }

            writer.printf("Regular Students: %d (%.1f%%)%n",
                    regularCount, totalStudents > 0 ? (regularCount * 100.0 / totalStudents) : 0);
            writer.printf("Honors Students: %d (%.1f%%)%n",
                    honorsCount, totalStudents > 0 ? (honorsCount * 100.0 / totalStudents) : 0);

            // Grade distribution
            writer.println("\n2. GRADE DISTRIBUTION");
            writer.println("-".repeat(40));

            if (gradeCount > 0) {
                int[] gradeRanges = new int[5];
                for (int i = 0; i < gradeCount; i++) {
                    if (grades[i] != null) {
                        double grade = grades[i].getGrade();
                        if (grade >= 90) gradeRanges[0]++;
                        else if (grade >= 80) gradeRanges[1]++;
                        else if (grade >= 70) gradeRanges[2]++;
                        else if (grade >= 60) gradeRanges[3]++;
                        else gradeRanges[4]++;
                    }
                }

                String[] rangeLabels = {
                        "90% - 100% (Excellent)",
                        "80% - 89%  (Very Good)",
                        "70% - 79%  (Good)",
                        "60% - 69%  (Satisfactory)",
                        "0%  - 59%  (Needs Improvement)"
                };

                for (int i = 0; i < gradeRanges.length; i++) {
                    double percentage = (gradeRanges[i] * 100.0) / gradeCount;
                    writer.printf("%-30s : %3d grades (%.1f%%)%n",
                            rangeLabels[i], gradeRanges[i], percentage);
                }
            }

            writer.println("\n3. STATISTICAL ANALYSIS");
            writer.println("-".repeat(40));

            if (gradeCount > 0) {
                // Calculate statistics (simplified for export)
                double sum = 0;
                double min = 100;
                double max = 0;
                int validCount = 0;

                for (int i = 0; i < gradeCount; i++) {
                    if (grades[i] != null) {
                        double grade = grades[i].getGrade();
                        sum += grade;
                        if (grade < min) min = grade;
                        if (grade > max) max = grade;
                        validCount++;
                    }
                }

                if (validCount > 0) {
                    double mean = sum / validCount;
                    writer.printf("Mean (Average): %.2f%%%n", mean);
                    writer.printf("Range: %.2f%% (%.1f to %.1f)%n", max - min, min, max);
                    writer.printf("Standard Deviation: %.2f%%%n",
                            calculateStandardDeviation(mean, validCount));
                }
            }

            writer.println("\n4. SUBJECT PERFORMANCE");
            writer.println("-".repeat(40));

            // Core subjects
            writer.println("\nCore Subjects:");
            for (Subject subj : subject) {
                if (subj != null && subj.getSubjectType().equals("Core")) {
                    double subjectTotal = 0;
                    int subjectCount = 0;

                    for (int i = 0; i < gradeCount; i++) {
                        if (grades[i] != null &&
                                grades[i].getSubject().getSubjectCode().equals(subj.getSubjectCode())) {
                            subjectTotal += grades[i].getGrade();
                            subjectCount++;
                        }
                    }

                    if (subjectCount > 0) {
                        writer.printf("  %-20s: %.1f%% (%d grades)%n",
                                subj.getSubjectName(), subjectTotal / subjectCount, subjectCount);
                    }
                }
            }

            writer.println("\nElective Subjects:");
            for (Subject subj : subject) {
                if (subj != null && subj.getSubjectType().equals("Elective")) {
                    double subjectTotal = 0;
                    int subjectCount = 0;

                    for (int i = 0; i < gradeCount; i++) {
                        if (grades[i] != null &&
                                grades[i].getSubject().getSubjectCode().equals(subj.getSubjectCode())) {
                            subjectTotal += grades[i].getGrade();
                            subjectCount++;
                        }
                    }

                    if (subjectCount > 0) {
                        writer.printf("  %-20s: %.1f%% (%d grades)%n",
                                subj.getSubjectName(), subjectTotal / subjectCount, subjectCount);
                    }
                }
            }

            writer.println("\n" + "=".repeat(60));
            writer.println("End of Report");

            System.out.println("✅ Statistics report exported to: " + fileName);

        } catch (java.io.IOException e) {
            System.out.println("❌ Error exporting report: " + e.getMessage());
        }
    }

    // Helper method to calculate standard deviation
    private double calculateStandardDeviation(double mean, int count) {
        if (count <= 0) return 0;

        double variance = 0;
        int validGrades = 0;

        for (int i = 0; i < gradeCount; i++) {
            if (grades[i] != null) {
                variance += Math.pow(grades[i].getGrade() - mean, 2);
                validGrades++;
            }
        }

        if (validGrades > 0) {
            variance /= validGrades;
            return Math.sqrt(variance);
        }

        return 0;
    }

    // SEARCH FUNCTIONALITY
    public void searchStudents() {
        do {
            System.out.println("\n=============================================");
            System.out.println("           SEARCH STUDENTS");
            System.out.println("=============================================");
            System.out.println("Search Options:");
            System.out.println("1. By Student ID");
            System.out.println("2. By Name (Partial Match)");
            System.out.println("3. By Grade Range");
            System.out.println("4. By Student Type (Regular/Honors)");
            System.out.println("5. Return to Main Menu");
            System.out.print("Enter choice (1-5): ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (Exception e) {
                System.out.println("Invalid input! Please enter a number.");
                continue;
            }

            if (choice == 5) {
                return;
            }

            Student[] searchResults = performSearch(choice);

            if (searchResults == null || searchResults.length == 0) {
                System.out.println("\nNo students found matching your criteria.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                continue;
            }

            displaySearchResults(searchResults);
            handleSearchResults(searchResults);

        } while (true);
    }

    private Student[] performSearch(int searchType) {
        List<Student> results = new ArrayList<>();

        Student[] allStudents = studentManager.getAllStudents();
        int totalStudents = studentManager.getStudentCount();

        switch (searchType) {
            case 1: // By Student ID
                System.out.print("\nEnter Student ID to search: ");
                String searchId = scanner.nextLine().trim().toUpperCase();

                for (int i = 0; i < totalStudents; i++) {
                    if (allStudents[i] != null &&
                            allStudents[i].getStudentId().toUpperCase().contains(searchId)) {
                        results.add(allStudents[i]);
                    }
                }
                break;

            case 2: // By Name (Partial Match)
                System.out.print("\nEnter name or part of name to search: ");
                String searchName = scanner.nextLine().trim().toLowerCase();

                for (int i = 0; i < totalStudents; i++) {
                    if (allStudents[i] != null &&
                            allStudents[i].getName().toLowerCase().contains(searchName)) {
                        results.add(allStudents[i]);
                    }
                }
                break;

            case 3: // By Grade Range
                System.out.println("\nSearch by Grade Range");
                System.out.println("Enter minimum grade (0-100): ");
                double minGrade = getValidGradeInput("min");
                System.out.println("Enter maximum grade (0-100): ");
                double maxGrade = getValidGradeInput("max");

                if (minGrade > maxGrade) {
                    System.out.println("Minimum grade cannot be greater than maximum grade.");
                    return new Student[0];
                }

                for (int i = 0; i < totalStudents; i++) {
                    if (allStudents[i] != null) {
                        double studentAvg = calculateOverallAverage(allStudents[i].getStudentId());
                        if (studentAvg >= minGrade && studentAvg <= maxGrade) {
                            results.add(allStudents[i]);
                        }
                    }
                }
                break;

            case 4: // By Student Type
                System.out.println("\nSearch by Student Type:");
                System.out.println("1. Regular Students");
                System.out.println("2. Honors Students");
                System.out.println("3. Both");
                System.out.print("Enter choice (1-3): ");

                int typeChoice;
                try {
                    typeChoice = Integer.parseInt(scanner.nextLine());
                } catch (Exception e) {
                    System.out.println("Invalid choice!");
                    return new Student[0];
                }

                String targetType = "";
                if (typeChoice == 1) targetType = "Regular";
                else if (typeChoice == 2) targetType = "Honors";

                for (int i = 0; i < totalStudents; i++) {
                    if (allStudents[i] != null) {
                        if (typeChoice == 3 || allStudents[i].getStudentType().equals(targetType)) {
                            results.add(allStudents[i]);
                        }
                    }
                }
                break;

            default:
                System.out.println("Invalid search option!");
                return new Student[0];
        }

        return results.toArray(new Student[0]);
    }

    private double getValidGradeInput(String type) {
        while (true) {
            try {
                System.out.print("Enter " + type + " grade (0-100): ");
                double grade = Double.parseDouble(scanner.nextLine());
                if (grade >= 0 && grade <= 100) {
                    return grade;
                } else {
                    System.out.println("Grade must be between 0 and 100.");
                }
            } catch (Exception e) {
                System.out.println("Invalid input! Please enter a number.");
            }
        }
    }

    private void displaySearchResults(Student[] results) {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("SEARCH RESULTS (" + results.length + " students found)");
        System.out.println("=".repeat(100));
        System.out.printf("%-5s %-12s %-25s %-15s %-12s %-10s %-15s%n",
                "No.", "Student ID", "Name", "Type", "Avg Grade", "Status", "Subjects");
        System.out.println("-".repeat(100));

        for (int i = 0; i < results.length; i++) {
            Student student = results[i];
            double avgGrade = calculateOverallAverage(student.getStudentId());
            int subjectCount = getRegisteredSubjects(student.getStudentId());
            String status = avgGrade >= student.getPassingGrade() ? "Passing ✅" : "Failing ❌";

            System.out.printf("%-5d %-12s %-25s %-15s %-11.1f%% %-10s %-15d%n",
                    i + 1,
                    student.getStudentId(),
                    student.getName(),
                    student.getStudentType(),
                    avgGrade,
                    status,
                    subjectCount);
        }
        System.out.println("-".repeat(100));
    }

    private void handleSearchResults(Student[] results) {
        System.out.println("\nOptions:");
        System.out.println("1. View full details for a student");
        System.out.println("2. Export search results");
        System.out.println("3. New search");
        System.out.println("4. Return to main menu");
        System.out.print("Enter choice (1-4): ");

        try {
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1:
                    viewStudentDetailsFromSearch(results);
                    break;
                case 2:
                    exportSearchResults(results);
                    break;
                case 3:
                    // Return to search menu (loop will continue)
                    break;
                case 4:
                    // Return to main menu
                    return;
                default:
                    System.out.println("Invalid choice! Returning to main menu.");
                    return;
            }
        } catch (Exception e) {
            System.out.println("Invalid input! Returning to main menu.");
        }
    }

    private void viewStudentDetailsFromSearch(Student[] results) {
        System.out.print("\nEnter the number of the student to view details (1-" + results.length + "): ");

        try {
            int studentNumber = Integer.parseInt(scanner.nextLine());

            if (studentNumber < 1 || studentNumber > results.length) {
                System.out.println("Invalid student number!");
                return;
            }

            Student selectedStudent = results[studentNumber - 1];
            displayStudentFullDetails(selectedStudent);

        } catch (Exception e) {
            System.out.println("Invalid input!");
        }
    }

    private void displayStudentFullDetails(Student student) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("STUDENT DETAILS");
        System.out.println("=".repeat(80));

        student.displayStudentDetails();

        double avgGrade = calculateOverallAverage(student.getStudentId());
        int subjectCount = getRegisteredSubjects(student.getStudentId());

        System.out.println("\nAcademic Information:");
        System.out.println("-".repeat(40));
        System.out.printf("Average Grade: %.1f%%%n", avgGrade);
        System.out.printf("Subjects Enrolled: %d%n", subjectCount);
        System.out.printf("Passing Requirement: %.0f%%%n", student.getPassingGrade());

        if (avgGrade >= student.getPassingGrade()) {
            System.out.println("Overall Status: PASSING ✅");
        } else {
            System.out.println("Overall Status: FAILING ❌");
        }

        // Display individual grades
        System.out.println("\nGrade Details:");
        System.out.println("-".repeat(60));
        System.out.printf("%-20s %-15s %-10s %-10s%n",
                "Subject", "Type", "Grade", "Status");
        System.out.println("-".repeat(60));

        int gradeCount = 0;
        for (int i = 0; i < this.gradeCount; i++) {
            Grade g = grades[i];
            if (g != null && g.getStudentId().equals(student.getStudentId())) {
                String gradeStatus = g.getGrade() >= student.getPassingGrade() ? "Pass ✅" : "Fail ❌";
                System.out.printf("%-20s %-15s %-9.1f%% %-10s%n",
                        g.getSubject().getSubjectName(),
                        g.getSubject().getSubjectType(),
                        g.getGrade(),
                        gradeStatus);
                gradeCount++;
            }
        }

        if (gradeCount == 0) {
            System.out.println("No grades recorded for this student.");
        }

        System.out.println("-".repeat(60));

        // Calculate core and elective averages
        double coreAvg = calculateCoreAverage(student.getStudentId());
        double electiveAvg = calculateElectiveAverage(student.getStudentId());

        System.out.printf("\nCore Subjects Average: %.1f%%%n", coreAvg);
        System.out.printf("Elective Subjects Average: %.1f%%%n", electiveAvg);

        // Additional info for honors students
        if (student.getStudentType().equals("Honors")) {
            boolean honorsEligible = avgGrade >= 85;
            System.out.printf("Honors Eligibility: %s%n",
                    honorsEligible ? "Eligible ✓" : "Not Eligible ✗");
            if (!honorsEligible) {
                System.out.printf("  (Required: 85%%, Current: %.1f%%)%n", avgGrade);
            }
        }

        System.out.println("\nOptions:");
        System.out.println("1. View grade report for this student");
        System.out.println("2. Calculate GPA for this student");
        System.out.println("3. Return to search results");
        System.out.print("Enter choice (1-3): ");

        try {
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1:
                    viewGradeByStudent(student.getStudentId());
                    break;
                case 2:
                    displayStudentGPA(student);
                    break;
                case 3:
                    // Return to search results
                    break;
            }
        } catch (Exception e) {
            // Continue
        }

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private void displayStudentGPA(Student student) {
        List<Grade> studentGrades = getGradesByStudentId(student.getStudentId());

        if (studentGrades.isEmpty()) {
            System.out.println("\nThis student has no recorded grades.");
            return;
        }

        System.out.println("\nGPA Calculation for " + student.getName());
        System.out.println("-".repeat(50));

        double totalGPA = 0;
        for (Grade g : studentGrades) {
            double percentage = g.getGrade();
            double courseGPA = GPACalculator.percentageToGPA(percentage);
            String letter = GPACalculator.gpaToLetter(courseGPA);
            totalGPA += courseGPA;

            System.out.printf("%-20s: %5.1f%% -> %4.2f (%s)%n",
                    g.getSubject().getSubjectName(),
                    percentage,
                    courseGPA,
                    letter);
        }

        double cumulativeGPA = totalGPA / studentGrades.size();
        String cumulativeLetter = GPACalculator.gpaToLetter(cumulativeGPA);

        System.out.println("-".repeat(50));
        System.out.printf("Cumulative GPA: %.2f / 4.0 (%s)%n",
                cumulativeGPA, cumulativeLetter);

        // Performance analysis
        System.out.println("\nPerformance Analysis:");
        if (cumulativeGPA >= 3.5) {
            System.out.println("✓ Excellent performance");
        } else if (cumulativeGPA >= 3.0) {
            System.out.println("✓ Good performance");
        } else if (cumulativeGPA >= 2.0) {
            System.out.println("✓ Satisfactory performance");
        } else {
            System.out.println("⚠ Needs improvement");
        }

        if (student.getStudentType().equals("Honors") && cumulativeGPA < 3.5) {
            System.out.println("⚠ Honors eligibility at risk (GPA < 3.5)");
        }
    }

    private void exportSearchResults(Student[] results) {
        System.out.println("\nExport Search Results");
        System.out.println("-".repeat(30));

        if (results.length == 0) {
            System.out.println("No results to export.");
            return;
        }

        System.out.print("Enter filename for export (without extension): ");
        String filename = scanner.nextLine().trim();

        if (filename.isEmpty()) {
            filename = "search_results_" + java.time.LocalDate.now().toString().replace("-", "");
        }

        String reportsDir = "./reports";
        java.io.File directory = new java.io.File(reportsDir);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                System.out.println("❌ Failed to create reports directory!");
                return;
            }
        }

        String filePath = reportsDir + "/" + filename + ".txt";

        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(filePath))) {
            // Write header
            writer.println("=".repeat(80));
            writer.println("SEARCH RESULTS EXPORT");
            writer.println("Generated: " + java.time.LocalDateTime.now());
            writer.println("Total Students Found: " + results.length);
            writer.println("=".repeat(80));
            writer.println();

            // Write search criteria
            writer.println("Search Criteria:");
            writer.println("-".repeat(40));
            writer.println("Search performed on: " + java.time.LocalDate.now());
            writer.println("Number of results: " + results.length);
            writer.println();

            // Write student details
            writer.println("STUDENT LIST:");
            writer.println("-".repeat(80));
            writer.printf("%-12s %-25s %-15s %-12s %-10s %-15s%n",
                    "Student ID", "Name", "Type", "Avg Grade", "Status", "Subjects");
            writer.println("-".repeat(80));

            for (Student student : results) {
                double avgGrade = calculateOverallAverage(student.getStudentId());
                int subjectCount = getRegisteredSubjects(student.getStudentId());
                String status = avgGrade >= student.getPassingGrade() ? "Passing" : "Failing";

                writer.printf("%-12s %-25s %-15s %-11.1f%% %-10s %-15d%n",
                        student.getStudentId(),
                        student.getName(),
                        student.getStudentType(),
                        avgGrade,
                        status,
                        subjectCount);
            }

            writer.println();
            writer.println("DETAILED INFORMATION:");
            writer.println("=".repeat(80));

            // Write detailed information for each student
            for (int i = 0; i < results.length; i++) {
                Student student = results[i];
                writer.println("\nStudent #" + (i + 1) + ":");
                writer.println("-".repeat(40));
                writer.println("ID: " + student.getStudentId());
                writer.println("Name: " + student.getName());
                writer.println("Type: " + student.getStudentType());
                writer.println("Email: " + student.getEmail());
                writer.println("Phone: " + student.getPhone());
                writer.println("Age: " + student.getAge());
                writer.println("Status: " + student.getStatus());

                double avgGrade = calculateOverallAverage(student.getStudentId());
                int subjectCount = getRegisteredSubjects(student.getStudentId());

                writer.println("\nAcademic Information:");
                writer.printf("  Average Grade: %.1f%%%n", avgGrade);
                writer.printf("  Subjects Enrolled: %d%n", subjectCount);
                writer.printf("  Passing Requirement: %.0f%%%n", student.getPassingGrade());
                writer.printf("  Overall Status: %s%n",
                        avgGrade >= student.getPassingGrade() ? "PASSING" : "FAILING");

                // Write individual grades
                List<Grade> studentGrades = getGradesByStudentId(student.getStudentId());
                if (!studentGrades.isEmpty()) {
                    writer.println("\n  Individual Grades:");
                    writer.println("  " + "-".repeat(50));
                    for (Grade grade : studentGrades) {
                        writer.printf("  %-20s: %5.1f%%%n",
                                grade.getSubject().getSubjectName(),
                                grade.getGrade());
                    }
                }
            }

            writer.println("\n" + "=".repeat(80));
            writer.println("End of Export");

            System.out.println("✅ Search results exported to: " + filePath);
            System.out.println("   Total students exported: " + results.length);

        } catch (java.io.IOException e) {
            System.out.println("❌ Error exporting results: " + e.getMessage());
        }

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
}