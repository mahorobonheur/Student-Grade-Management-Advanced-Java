package org.example.newImprementations;

import org.example.student.Student;
import org.example.grade.Grade;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class FileExporter {

    // 3-parameter version (for backward compatibility with StudentManager)
    public static void exportStudentToFile(Student student, double averageGrade, int subjectCount) {
        String studentFileName = student.getName().toLowerCase()
                .replace(" ", "_")
                .replaceAll("[^a-z_]", "") + "_report";
        exportStudentToFile(student, averageGrade, subjectCount, studentFileName, true);
    }

    // 5-parameter version (for GradeManager with custom filename)
    public static File exportStudentToFile(Student student, double averageGrade, int subjectCount,
                                           String baseFileName, boolean includeGrades) {
        String reportsDir = "./reports";
        File directory = new File(reportsDir);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                System.out.println("❌ Failed to create reports directory!");
                return null;
            }
        }

        String fileName = reportsDir + "/" + baseFileName + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);

            writer.println("=".repeat(60));
            writer.println("STUDENT ACADEMIC REPORT");
            writer.println("=".repeat(60));
            writer.printf("Generated on: %s%n%n", timestamp);

            writer.println("STUDENT INFORMATION");
            writer.println("-".repeat(40));
            writer.printf("Student ID: %s%n", student.getStudentId());
            writer.printf("Name: %s%n", student.getName());
            writer.printf("Type: %s Student%n", student.getStudentType());
            writer.printf("Age: %d%n", student.getAge());
            writer.printf("Email: %s%n", student.getEmail());
            writer.printf("Phone: %s%n", student.getPhone());
            writer.printf("Status: %s%n", student.getStatus());
            writer.printf("Passing Requirement: %.0f%%%n%n", student.getPassingGrade());

            writer.println("ACADEMIC SUMMARY");
            writer.println("-".repeat(40));
            writer.printf("Enrolled Subjects: %d%n", subjectCount);
            writer.printf("Current Average: %.1f%%%n", averageGrade);

            if (averageGrade >= student.getPassingGrade()) {
                writer.println("Overall Status: PASSING ✅");
            } else {
                writer.println("Overall Status: FAILING ❌");
            }

            if (includeGrades) {
                writer.println();
                writer.println("GRADE DETAILS");
                writer.println("-".repeat(40));
                writer.println("Individual grades are recorded in the student's grade history.");
                writer.println("Total grades recorded: " + subjectCount);
                writer.println("Average calculated from all recorded grades.");
            }

            writer.println();
            writer.println("=".repeat(60));
            writer.println("End of Report");

            File exportedFile = new File(fileName);
            if (exportedFile.exists()) {
                return exportedFile;
            }

        } catch (IOException e) {
            System.out.println("❌ Error saving student report: " + e.getMessage());
        }

        return null;
    }

    // Method to handle grade updates in student files
    public static void updateStudentGradeFile(Student student, Grade newGrade, double newAverage, int totalSubjects) {
        String reportsDir = "./reports";
        File directory = new File(reportsDir);

        if (!directory.exists()) {
            return;
        }

        String studentFileName = student.getName().toLowerCase()
                .replace(" ", "_")
                .replaceAll("[^a-z_]", "") + "_report.txt";

        String fileName = reportsDir + "/" + studentFileName;

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            writer.println();
            writer.println("Grade Update on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.printf("New Grade Added: %s - %.1f%%%n",
                    newGrade.getSubject().getSubjectName(),
                    newGrade.getGrade());
            writer.printf("Updated Average: %.1f%%%n", newAverage);
            writer.printf("Total Subjects: %d%n", totalSubjects);
            writer.println("-".repeat(40));

        } catch (IOException e) {
            // Silently fail - this is just an update
        }
    }

    public static void exportAllStudentsToFile(Student[] students, int studentCount, double classAverage) {
        String reportsDir = "./reports";
        File directory = new File(reportsDir);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                System.out.println("❌ Failed to create reports directory!");
                return;
            }
        }

        String fileName = reportsDir + "/all_students_report.txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);

            writer.println("=".repeat(80));
            writer.println("ALL STUDENTS ACADEMIC REPORT");
            writer.println("=".repeat(80));
            writer.printf("Generated on: %s%n%n", timestamp);

            writer.println("SUMMARY STATISTICS");
            writer.println("-".repeat(40));
            writer.printf("Total Students: %d%n", studentCount);
            writer.printf("Class Average: %.1f%%%n%n", classAverage);

            writer.println("STUDENT LISTING");
            writer.println("-".repeat(80));
            writer.printf("%-12s %-20s %-15s %-12s %-10s%n",
                    "Student ID", "Name", "Type", "Average", "Status");
            writer.println("-".repeat(80));

            for (int i = 0; i < studentCount; i++) {
                Student student = students[i];
                if (student != null) {
                    double avg = student.calculateAverageGrade();
                    String status = avg >= student.getPassingGrade() ? "PASS" : "FAIL";

                    writer.printf("%-12s %-20s %-15s %-11.1f%% %-10s%n",
                            student.getStudentId(),
                            student.getName(),
                            student.getStudentType(),
                            avg,
                            status);
                }
            }

            writer.println();
            writer.println("=".repeat(80));
            writer.println("End of Report");

            System.out.println("✅ All students report saved to: " + fileName);

        } catch (IOException e) {
            System.out.println("❌ Error saving all students report: " + e.getMessage());
        }
    }

    public static void exportAllGradesToCSV(Grade[] grades, int gradeCount, Student[] students, int studentCount) {
        String importsDir = "./imports";
        File directory = new File(importsDir);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                System.out.println("❌ Failed to create imports directory!");
                return;
            }
        }

        String fileName = importsDir + "/grades_export_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                ".csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("GRADE_ID,STUDENT_ID,STUDENT_NAME,STUDENT_TYPE,SUBJECT_CODE,SUBJECT_NAME,SUBJECT_TYPE,GRADE,DATE");

            int exportedCount = 0;

            for (int i = 0; i < gradeCount; i++) {
                Grade grade = grades[i];
                if (grade != null) {
                    String studentName = "Unknown";
                    String studentType = "Unknown";

                    for (int j = 0; j < studentCount; j++) {
                        if (students[j] != null && students[j].getStudentId().equals(grade.getStudentId())) {
                            studentName = students[j].getName();
                            studentType = students[j].getStudentType();
                            break;
                        }
                    }

                    writer.printf("%s,%s,%s,%s,%s,%s,%s,%.2f,%s%n",
                            grade.getGradeId(),
                            grade.getStudentId(),
                            escapeCSV(studentName),
                            studentType,
                            grade.getSubject().getSubjectCode(),
                            escapeCSV(grade.getSubject().getSubjectName()),
                            grade.getSubject().getSubjectType(),
                            grade.getGrade(),
                            grade.getDate());

                    exportedCount++;
                }
            }

            System.out.println("✅ Exported " + exportedCount + " grades to CSV file: " + fileName);

        } catch (IOException e) {
            System.out.println("❌ Error exporting grades to CSV: " + e.getMessage());
        }
    }

    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}