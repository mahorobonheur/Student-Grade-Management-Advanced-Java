package org.example.application.service;

import org.example.infrastructure.util.ValidationUtils;

public class ValidationService {

    public ValidationResult validateStudentId(String studentId) {
        return ValidationUtils.validateStudentId(studentId);
    }

    public ValidationResult validateEmail(String email) {
        return ValidationUtils.validateEmail(email);
    }

    public ValidationResult validatePhone(String phone) {
        return ValidationUtils.validatePhone(phone);
    }

    public ValidationResult validateName(String name) {
        return ValidationUtils.validateName(name);
    }

    public ValidationResult validateDate(String date) {
        return ValidationUtils.validateDate(date);
    }

    public ValidationResult validateCourseCode(String courseCode) {
        return ValidationUtils.validateCourseCode(courseCode);
    }

    public ValidationResult validateGrade(String grade) {
        return ValidationUtils.validateGrade(grade);
    }

    // Result wrapper class
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}