package org.example.application.service;

import org.example.domain.model.Student;
import org.example.domain.model.Grade;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExportService {

    public void exportStudentToFile(Student student, double averageGrade, int subjectCount) {
        String fileName = "./reports/student_" + student.getStudentId() + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("STUDENT REPORT");
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writer.println("=".repeat(50));
            writer.println("ID: " + student.getStudentId());
            writer.println("Name: " + student.getName());
            writer.println("Type: " + student.getStudentType());
            writer.println("Average Grade: " + String.format("%.1f%%", averageGrade));
            writer.println("Subjects: " + subjectCount);
            writer.println("Status: " + student.getStatus());

            System.out.println("Report exported to: " + fileName);
        } catch (IOException e) {
            System.out.println("Failed to export report: " + e.getMessage());
        }
    }

    public void updateStudentGradeFile(Student student, Grade grade, double newAverage, int totalSubjects) {
        exportStudentToFile(student, newAverage, totalSubjects);
    }
}