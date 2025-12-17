package org.example.infrastructure.util;

import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^STU\\d{3}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\(\\d{3}\\) \\d{3}-\\d{4}|\\d{3}-\\d{3}-\\d{4}|\\+1-\\d{3}-\\d{3}-\\d{4}|\\d{10})$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z]+([-'\\s][a-zA-Z]+)*$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile("^[A-Z]{3}\\d{3}$");
    private static final Pattern GRADE_PATTERN = Pattern.compile("^(100|[1-9]?\\d|0)$");
    private static final Pattern EMAIL_DOMAIN_PATTERN = Pattern.compile("^.*@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})$");
    private static final Pattern AREA_CODE_PATTERN = Pattern.compile("^\\(?(\\d{3})\\)?");

    public static boolean isValidEmailDomain(String email, String domain) {
        if (!EMAIL_PATTERN.matcher(email).matches()) return false;
        return email.toLowerCase().endsWith(domain.toLowerCase());
    }

    public static String extractDomainFromEmail(String email) {
        var matcher = EMAIL_DOMAIN_PATTERN.matcher(email);
        return matcher.matches() ? matcher.group(1) : null;
    }

    public static String extractAreaCode(String phone) {
        var matcher = AREA_CODE_PATTERN.matcher(phone);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static org.example.application.service.ValidationService.ValidationResult
    validateStudentId(String studentId) {
        if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            return org.example.application.service.ValidationService.ValidationResult.failure(
                    "Student ID must be in format STUXXX (e.g., STU001)");
        }
        return org.example.application.service.ValidationService.ValidationResult.success();
    }

    public static org.example.application.service.ValidationService.ValidationResult
    validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return org.example.application.service.ValidationService.ValidationResult.failure(
                    "Invalid email format (e.g., user@example.com)");
        }
        return org.example.application.service.ValidationService.ValidationResult.success();
    }

    public static org.example.application.service.ValidationService.ValidationResult
    validatePhone(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return org.example.application.service.ValidationService.ValidationResult.failure(
                    "Invalid phone format. Use: (123) 456-7890, 123-456-7890, or 1234567890");
        }
        return org.example.application.service.ValidationService.ValidationResult.success();
    }

    public static org.example.application.service.ValidationService.ValidationResult
    validateName(String name) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            return org.example.application.service.ValidationService.ValidationResult.failure(
                    "Name can only contain letters, spaces, hyphens, and apostrophes");
        }
        return org.example.application.service.ValidationService.ValidationResult.success();
    }

    public static org.example.application.service.ValidationService.ValidationResult
    validateDate(String date) {
        if (!DATE_PATTERN.matcher(date).matches()) {
            return org.example.application.service.ValidationService.ValidationResult.failure(
                    "Date must be in YYYY-MM-DD format");
        }
        return org.example.application.service.ValidationService.ValidationResult.success();
    }

    public static org.example.application.service.ValidationService.ValidationResult
    validateCourseCode(String courseCode) {
        if (!COURSE_CODE_PATTERN.matcher(courseCode).matches()) {
            return org.example.application.service.ValidationService.ValidationResult.failure(
                    "Course code must be 3 uppercase letters + 3 digits (e.g., MAT101)");
        }
        return org.example.application.service.ValidationService.ValidationResult.success();
    }

    public static org.example.application.service.ValidationService.ValidationResult
    validateGrade(String grade) {
        if (!GRADE_PATTERN.matcher(grade).matches()) {
            return org.example.application.service.ValidationService.ValidationResult.failure(
                    "Grade must be between 0 and 100");
        }
        return org.example.application.service.ValidationService.ValidationResult.success();
    }
}