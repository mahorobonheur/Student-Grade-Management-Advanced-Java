package org.example.application.service;

import org.example.domain.model.Student;
import org.example.domain.model.Grade;
import org.example.domain.repository.StudentRepository;
import org.example.domain.repository.GradeRepository;

import java.util.List;
import java.util.stream.Collectors;

public class StatisticsService {

    public void generateClassReport(StudentRepository studentRepo, GradeRepository gradeRepo) {
        List<Student> students = studentRepo.findAll();

        System.out.println("\n📊 CLASS STATISTICS REPORT");
        System.out.println("=".repeat(60));

        System.out.println("Total Students: " + students.size());

        long regularCount = students.stream()
                .filter(s -> s.getStudentType().equals("Regular"))
                .count();
        long honorsCount = students.size() - regularCount;

        System.out.println("Regular Students: " + regularCount);
        System.out.println("Honors Students: " + honorsCount);

        double classAverage = calculateClassAverage(students, gradeRepo);
        System.out.printf("Class Average: %.1f%%%n", classAverage);

        System.out.println("\nPerformance Distribution:");
        printGradeDistribution(students, gradeRepo);
    }

    private double calculateClassAverage(List<Student> students, GradeRepository gradeRepo) {
        if (students.isEmpty()) return 0.0;

        double total = students.stream()
                .mapToDouble(s -> gradeRepo.calculateStudentAverage(s.getStudentId()))
                .sum();
        return total / students.size();
    }

    private void printGradeDistribution(List<Student> students, GradeRepository gradeRepo) {
        int[] ranges = new int[5];

        for (Student student : students) {
            double avg = gradeRepo.calculateStudentAverage(student.getStudentId());

            if (avg >= 90) ranges[0]++;
            else if (avg >= 80) ranges[1]++;
            else if (avg >= 70) ranges[2]++;
            else if (avg >= 60) ranges[3]++;
            else ranges[4]++;
        }

        String[] labels = {"A (90-100%)", "B (80-89%)", "C (70-79%)", "D (60-69%)", "F (<60%)"};

        for (int i = 0; i < ranges.length; i++) {
            double percentage = (ranges[i] * 100.0) / students.size();
            System.out.printf("%-12s: %d students (%.1f%%)%n", labels[i], ranges[i], percentage);
        }
    }
}