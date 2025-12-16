package org.example.newImprementations;

import org.example.grade.Grade;
import org.example.student.Student;

import java.util.*;

public class GPACalculator {


    public static double percentageToGPA(double percentage) {
        if (percentage >= 93) return 4.0;
        if (percentage >= 90) return 3.7;
        if (percentage >= 87) return 3.3;
        if (percentage >= 83) return 3.0;
        if (percentage >= 80) return 2.7;
        if (percentage >= 77) return 2.3;
        if (percentage >= 73) return 2.0;
        if (percentage >= 70) return 1.7;
        if (percentage >= 67) return 1.3;
        if (percentage >= 60) return 1.0;
        return 0.0;
    }


    public static String gpaToLetter(double gpa) {
        if (gpa >= 4.0) return "A";
        if (gpa >= 3.7) return "A–";
        if (gpa >= 3.3) return "B+";
        if (gpa >= 3.0) return "B";
        if (gpa >= 2.7) return "B–";
        if (gpa >= 2.3) return "C+";
        if (gpa >= 2.0) return "C";
        if (gpa >= 1.7) return "C–";
        if (gpa >= 1.3) return "D+";
        if (gpa >= 1.0) return "D";
        return "F";
    }


    public static double calculateSubjectGPA(Grade grade) {
        return percentageToGPA(grade.getGrade());
    }


    public static double calculateCumulativeGPA(List<Grade> grades) {
        if (grades.isEmpty()) return 0.0;

        double total = 0;
        for (Grade g : grades) {
            total += percentageToGPA(g.getGrade());
        }

        return Math.round((total / grades.size()) * 100.0) / 100.0;
    }


    public static int calculateClassRank(Student student, List<Student> allStudents, Map<Student, Double> studentAverages) {


        List<Double> allGPAs = new ArrayList<>();
        for (Student s : allStudents) {
            allGPAs.add(percentageToGPA(studentAverages.get(s)));
        }

        allGPAs.sort(Collections.reverseOrder());

        double targetGPA = percentageToGPA(studentAverages.get(student));
        return allGPAs.indexOf(targetGPA) + 1;
    }

    public static List<String> performanceAnalysis(double gpa, double classAverage) {
        List<String> messages = new ArrayList<>();

        if (gpa >= 3.5)
            messages.add("✓ Excellent performance (3.5+ GPA)");

        if (gpa >= 3.0)
            messages.add("✓ Honors eligibility maintained");

        if (gpa > classAverage)
            messages.add("✓ Above class average (" + classAverage + " GPA)");

        if (messages.isEmpty())
            messages.add("✓ Keep improving!");

        return messages;
    }
}
