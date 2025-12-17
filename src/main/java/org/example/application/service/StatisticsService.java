package org.example.application.service;

import org.example.domain.model.*;
import org.example.domain.repository.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StatisticsService provides comprehensive statistical analysis
 * including mean, median, mode, range, standard deviation, and grade distribution
 */
public class StatisticsService {

    /**
     * Generates a comprehensive class statistics report
     * @param studentRepo Student repository
     * @param gradeRepo Grade repository
     */
    public void generateClassReport(StudentRepository studentRepo, GradeRepository gradeRepo) {
        List<Student> students = studentRepo.findAll();

        System.out.println("\nCLASS STATISTICS REPORT");
        System.out.println("=".repeat(70));

        if (students.isEmpty()) {
            System.out.println("No students found in the system.");
            return;
        }

        // Basic information
        System.out.println("Total Students: " + students.size());

        long regularCount = students.stream()
                .filter(s -> s.getStudentType().equals("Regular"))
                .count();
        long honorsCount = students.size() - regularCount;

        System.out.println("Regular Students: " + regularCount);
        System.out.println("Honors Students: " + honorsCount);

        // Collect all grades for statistical analysis
        List<Double> allGrades = new ArrayList<>();
        Map<String, List<Double>> subjectGrades = new HashMap<>();

        for (Student student : students) {
            List<Grade> grades = gradeRepo.findByStudentId(student.getStudentId());
            for (Grade grade : grades) {
                double gradeValue = grade.getGrade();
                allGrades.add(gradeValue);

                String subject = grade.getSubject().getSubjectName();
                subjectGrades.computeIfAbsent(subject, k -> new ArrayList<>())
                        .add(gradeValue);
            }
        }

        if (allGrades.isEmpty()) {
            System.out.println("\nNo grades recorded in the system.");
            return;
        }

        // Overall statistics
        System.out.println("\nOVERALL GRADE STATISTICS");
        System.out.println("-".repeat(40));
        System.out.printf("Total Grades: %d%n", allGrades.size());
        System.out.printf("Mean (Average): %.2f%%%n", calculateMean(allGrades));
        System.out.printf("Median: %.2f%%%n", calculateMedian(allGrades));

        String mode = calculateMode(allGrades);
        System.out.printf("Mode: %s%n", mode);

        System.out.printf("Range: %.2f%%%n", calculateRange(allGrades));
        System.out.printf("Standard Deviation: %.2f%%%n", calculateStandardDeviation(allGrades));
        System.out.printf("Variance: %.2f%%%n", calculateVariance(allGrades));

        // Grade distribution
        System.out.println("\nGRADE DISTRIBUTION");
        System.out.println("-".repeat(40));
        Map<String, Long> distribution = calculateGradeDistribution(students, gradeRepo);
        distribution.forEach((range, count) -> {
            double percentage = (count * 100.0) / students.size();
            System.out.printf("%-15s: %3d students (%5.1f%%)%n", range, count, percentage);
        });

        // Subject-wise statistics
        System.out.println("\nSUBJECT-WISE STATISTICS");
        System.out.println("-".repeat(40));
        System.out.printf("%-20s %-10s %-10s %-10s %-10s%n",
                "Subject", "Avg", "Min", "Max", "Count");
        System.out.println("-".repeat(60));

        for (Map.Entry<String, List<Double>> entry : subjectGrades.entrySet()) {
            String subject = entry.getKey();
            List<Double> grades = entry.getValue();

            double avg = calculateMean(grades);
            double min = Collections.min(grades);
            double max = Collections.max(grades);

            System.out.printf("%-20s %-9.1f%% %-9.1f%% %-9.1f%% %-10d%n",
                    subject, avg, min, max, grades.size());
        }

        // Student performance analysis
        System.out.println("\nSTUDENT PERFORMANCE ANALYSIS");
        System.out.println("-".repeat(40));

        List<StudentPerformance> performances = new ArrayList<>();
        for (Student student : students) {
            double avg = gradeRepo.calculateStudentAverage(student.getStudentId());
            performances.add(new StudentPerformance(student, avg));
        }

        // Sort by average grade
        performances.sort((p1, p2) -> Double.compare(p2.average, p1.average));

        System.out.println("Top 5 Students:");
        for (int i = 0; i < Math.min(5, performances.size()); i++) {
            StudentPerformance perf = performances.get(i);
            System.out.printf("%d. %-20s: %6.2f%% (%s)%n",
                    i + 1, perf.student.getName(), perf.average,
                    perf.average >= perf.student.getPassingGrade() ? "PASS" : "FAIL");
        }

        System.out.println("\nBottom 5 Students:");
        for (int i = Math.max(0, performances.size() - 5); i < performances.size(); i++) {
            StudentPerformance perf = performances.get(i);
            System.out.printf("%d. %-20s: %6.2f%% (%s)%n",
                    performances.size() - i, perf.student.getName(), perf.average,
                    perf.average >= perf.student.getPassingGrade() ? "PASS" : "FAIL");
        }

        // Pass/Fail statistics
        long passingCount = performances.stream()
                .filter(p -> p.average >= p.student.getPassingGrade())
                .count();
        long failingCount = performances.size() - passingCount;

        System.out.println("\n✅ PASS/FAIL STATISTICS");
        System.out.println("-".repeat(40));
        System.out.printf("Passing: %d (%.1f%%)%n",
                passingCount, (passingCount * 100.0) / performances.size());
        System.out.printf("Failing: %d (%.1f%%)%n",
                failingCount, (failingCount * 100.0) / performances.size());
    }

    /**
     * Calculates mean of a list of numbers
     * @param numbers List of numbers
     * @return Mean value
     */
    private double calculateMean(List<Double> numbers) {
        if (numbers.isEmpty()) return 0.0;
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
        if (numbers.isEmpty()) return 0.0;

        List<Double> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        int size = sorted.size();

        if (size % 2 == 0) {
            // Even number of elements
            return (sorted.get(size/2 - 1) + sorted.get(size/2)) / 2.0;
        } else {
            // Odd number of elements
            return sorted.get(size/2);
        }
    }

    /**
     * Calculates mode of a list of numbers
     * @param numbers List of numbers
     * @return Mode as string representation
     */
    private String calculateMode(List<Double> numbers) {
        if (numbers.isEmpty()) return "N/A";

        // Group numbers by frequency
        Map<Double, Long> frequencyMap = numbers.stream()
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));

        // Find maximum frequency
        long maxFrequency = frequencyMap.values().stream()
                .max(Long::compare)
                .orElse(0L);

        // If all frequencies are 1, there's no mode
        if (maxFrequency <= 1) {
            return "No mode (all values unique)";
        }

        // Collect all values with maximum frequency
        List<Double> modes = frequencyMap.entrySet().stream()
                .filter(entry -> entry.getValue() == maxFrequency)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (modes.size() == 1) {
            return String.format("%.2f (appears %d times)", modes.get(0), maxFrequency);
        } else {
            return modes.stream()
                    .map(d -> String.format("%.2f", d))
                    .collect(Collectors.joining(", ")) +
                    String.format(" (each appears %d times)", maxFrequency);
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
        double sumSquaredDifferences = numbers.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .sum();

        double variance = sumSquaredDifferences / (numbers.size() - 1);
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
     * Calculates grade distribution across letter grades
     * @param students List of students
     * @param gradeRepo Grade repository
     * @return Map of grade ranges to student counts
     */
    private Map<String, Long> calculateGradeDistribution(List<Student> students, GradeRepository gradeRepo) {
        Map<String, Long> distribution = new LinkedHashMap<>();

        // Initialize all grade ranges
        String[] ranges = {"A (90-100%)", "B (80-89%)", "C (70-79%)", "D (60-69%)", "F (<60%)"};
        for (String range : ranges) {
            distribution.put(range, 0L);
        }

        // Count students in each range
        for (Student student : students) {
            double average = gradeRepo.calculateStudentAverage(student.getStudentId());

            if (average >= 90) {
                distribution.put("A (90-100%)", distribution.get("A (90-100%)") + 1);
            } else if (average >= 80) {
                distribution.put("B (80-89%)", distribution.get("B (80-89%)") + 1);
            } else if (average >= 70) {
                distribution.put("C (70-79%)", distribution.get("C (70-79%)") + 1);
            } else if (average >= 60) {
                distribution.put("D (60-69%)", distribution.get("D (60-69%)") + 1);
            } else {
                distribution.put("F (<60%)", distribution.get("F (<60%)") + 1);
            }
        }

        return distribution;
    }

    /**
     * Inner class for tracking student performance
     */
    private static class StudentPerformance {
        Student student;
        double average;

        StudentPerformance(Student student, double average) {
            this.student = student;
            this.average = average;
        }
    }
}