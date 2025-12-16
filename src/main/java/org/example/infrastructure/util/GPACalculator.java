package org.example.infrastructure.util;

public class GPACalculator {

    public static double percentageToGPA(double percentage) {
        if (percentage >= 97) return 4.0;
        if (percentage >= 93) return 4.0;
        if (percentage >= 90) return 3.7;
        if (percentage >= 87) return 3.3;
        if (percentage >= 83) return 3.0;
        if (percentage >= 80) return 2.7;
        if (percentage >= 77) return 2.3;
        if (percentage >= 73) return 2.0;
        if (percentage >= 70) return 1.7;
        if (percentage >= 67) return 1.3;
        if (percentage >= 65) return 1.0;
        return 0.0;
    }

    public static String gpaToLetter(double gpa) {
        if (gpa >= 4.0) return "A+";
        if (gpa >= 3.7) return "A";
        if (gpa >= 3.3) return "A-";
        if (gpa >= 3.0) return "B+";
        if (gpa >= 2.7) return "B";
        if (gpa >= 2.3) return "B-";
        if (gpa >= 2.0) return "C+";
        if (gpa >= 1.7) return "C";
        if (gpa >= 1.3) return "C-";
        if (gpa >= 1.0) return "D";
        return "F";
    }
}