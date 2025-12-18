package unit.collections;

import org.example.infrastructure.util.ValidationUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Regex Validation Tests")
class RegexValidationTest {

    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^STU\\d{3}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\(\\d{3}\\) \\d{3}-\\d{4}|\\d{3}-\\d{3}-\\d{4}|\\+1-\\d{3}-\\d{3}-\\d{4}|\\d{10})$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z]+([-'\\s][a-zA-Z]+)*$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile("^[A-Z]{3}\\d{3}$");
    private static final Pattern GRADE_PATTERN = Pattern.compile("^(100|[1-9]?\\d|0)$");

    @ParameterizedTest
    @MethodSource("validStudentIds")
    @DisplayName("Valid Student ID Patterns")
    void testValidStudentIdPatterns(String studentId) {
        assertTrue(STUDENT_ID_PATTERN.matcher(studentId).matches(),
                "Should match valid student ID: " + studentId);

        var result = ValidationUtils.validateStudentId(studentId);
        assertTrue(result.isValid(), "Validation should pass for: " + studentId);
    }

    static Stream<String> validStudentIds() {
        return Stream.of(
                "STU001", "STU123", "STU999",
                "STU100", "STU500", "STU777",
                "STU010", "STU099", "STU888"
        );
    }

    @ParameterizedTest
    @MethodSource("invalidStudentIds")
    @DisplayName("Invalid Student ID Patterns")
    void testInvalidStudentIdPatterns(String studentId) {
        assertFalse(STUDENT_ID_PATTERN.matcher(studentId).matches(),
                "Should not match invalid student ID: " + studentId);

        var result = ValidationUtils.validateStudentId(studentId);
        assertFalse(result.isValid(), "Validation should fail for: " + studentId);
        assertNotNull(result.getErrorMessage());
    }

    static Stream<String> invalidStudentIds() {
        return Stream.of(
                "", "STU", "STU1", "STU12", "STU1234",
                "stu123", "STUABC", "STU12A", "STU-123",
                "ABC123", "  STU123  ", null
        );
    }

    @ParameterizedTest
    @MethodSource("validEmails")
    @DisplayName("Valid Email Patterns")
    void testValidEmailPatterns(String email) {
        assertTrue(EMAIL_PATTERN.matcher(email).matches(),
                "Should match valid email: " + email);

        var result = ValidationUtils.validateEmail(email);
        assertTrue(result.isValid(), "Validation should pass for: " + email);
    }

    static Stream<String> validEmails() {
        return Stream.of(
                "user@example.com",
                "first.last@company.co.uk",
                "user123@sub.domain.org",
                "user+tag@example.com",
                "user_name@domain.travel",
                "user-name@domain.info",
                "u@d.c",
                "user@123.456.789.123",  // IP address domain
                "user@example-example.com"
        );
    }

    @ParameterizedTest
    @MethodSource("invalidEmails")
    @DisplayName("Invalid Email Patterns")
    void testInvalidEmailPatterns(String email) {
        assertFalse(EMAIL_PATTERN.matcher(email).matches(),
                "Should not match invalid email: " + email);

        var result = ValidationUtils.validateEmail(email);
        assertFalse(result.isValid(), "Validation should fail for: " + email);
    }

    static Stream<String> invalidEmails() {
        return Stream.of(
                "", "@", "user@", "@domain.com",
                "user@.com", "user@domain.", "user@domain..com",
                "user name@domain.com", "user@domain_com",
                "user@domain.c",  // TLD too short
                "user@-domain.com",  // leading hyphen
                "user@domain-.com",  // trailing hyphen
                null
        );
    }

    @Test
    @DisplayName("Email Domain Validation")
    void testEmailDomainValidation() {
        assertTrue(ValidationUtils.isValidEmailDomain("user@university.edu", "university.edu"));
        assertTrue(ValidationUtils.isValidEmailDomain("admin@UNIVERSITY.EDU", "university.edu"));
        assertFalse(ValidationUtils.isValidEmailDomain("user@gmail.com", "university.edu"));
        assertFalse(ValidationUtils.isValidEmailDomain("invalid-email", "university.edu"));

        assertEquals("example.com",
                ValidationUtils.extractDomainFromEmail("user@example.com"));
        assertEquals("sub.domain.co.uk",
                ValidationUtils.extractDomainFromEmail("user@sub.domain.co.uk"));
        assertNull(ValidationUtils.extractDomainFromEmail("invalid-email"));
    }

    @ParameterizedTest
    @MethodSource("validPhones")
    @DisplayName("Valid Phone Patterns")
    void testValidPhonePatterns(String phone) {
        assertTrue(PHONE_PATTERN.matcher(phone).matches(),
                "Should match valid phone: " + phone);

        var result = ValidationUtils.validatePhone(phone);
        assertTrue(result.isValid(), "Validation should pass for: " + phone);
    }

    static Stream<String> validPhones() {
        return Stream.of(
                "(123) 456-7890",
                "123-456-7890",
                "1234567890",
                "+1-123-456-7890",
                "123-456-7890",
                "123 456 7890"
        );
    }

    @ParameterizedTest
    @MethodSource("invalidPhones")
    @DisplayName("Invalid Phone Patterns")
    void testInvalidPhonePatterns(String phone) {
        assertFalse(PHONE_PATTERN.matcher(phone).matches(),
                "Should not match invalid phone: " + phone);

        var result = ValidationUtils.validatePhone(phone);
        assertFalse(result.isValid(), "Validation should fail for: " + phone);
    }

    static Stream<String> invalidPhones() {
        return Stream.of(
                "", "123", "123-456", "123-456-78",
                "(123)456-7890",  // missing space
                "123-456-78901",  // too long
                "abc-def-ghij",   // letters
                "123-45-6789",    // wrong format
                null
        );
    }

    @Test
    @DisplayName("Phone Area Code Extraction")
    void testAreaCodeExtraction() {
        assertEquals("123", ValidationUtils.extractAreaCode("(123) 456-7890"));
        assertEquals("123", ValidationUtils.extractAreaCode("123-456-7890"));
        assertEquals("123", ValidationUtils.extractAreaCode("1234567890"));
        assertEquals("123", ValidationUtils.extractAreaCode("+1-123-456-7890"));
        assertNull(ValidationUtils.extractAreaCode("invalid"));
    }

    @ParameterizedTest
    @CsvSource({
            "John, true",
            "Mary-Jane, true",
            "O'Connor, true",
            "Jean Luc, true",
            "X Æ A-12, false",  // contains numbers and special characters
            "'', false",
            "John123, false",
            "John@Doe, false"
    })
    @DisplayName("Name Pattern Validation")
    void testNamePattern(String name, boolean expected) {
        assertEquals(expected, NAME_PATTERN.matcher(name).matches(),
                "Name validation failed for: " + name);
    }

    @ParameterizedTest
    @CsvSource({
            "MAT101, true",
            "ENG101, true",
            "SCI999, true",
            "mat101, false",  // lowercase
            "MAT10, false",   // missing digit
            "MAT1001, false", // too long
            "MA101, false",   // too few letters
            "MATH101, false"  // too many letters
    })
    @DisplayName("Course Code Pattern Validation")
    void testCourseCodePattern(String code, boolean expected) {
        assertEquals(expected, COURSE_CODE_PATTERN.matcher(code).matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "50", "100", "99", "1", "75"})
    @DisplayName("Valid Grade Patterns")
    void testValidGrades(String grade) {
        assertTrue(GRADE_PATTERN.matcher(grade).matches());
        assertTrue(ValidationUtils.validateGrade(grade).isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "101", "abc", "1.5", ""})
    @DisplayName("Invalid Grade Patterns")
    void testInvalidGrades(String grade) {
        assertFalse(GRADE_PATTERN.matcher(grade).matches());
        assertFalse(ValidationUtils.validateGrade(grade).isValid());
    }

    @Test
    @DisplayName("Pattern Compilation Performance")
    void testPatternCompilationPerformance() {
        // Test compiled pattern reuse vs recompilation
        int iterations = 10_000;

        // With recompilation
        long startWithRecompilation = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Pattern.compile("^STU\\d{3}$").matcher("STU123").matches();
        }
        long timeWithRecompilation = System.nanoTime() - startWithRecompilation;

        // With precompiled pattern
        Pattern precompiled = Pattern.compile("^STU\\d{3}$");
        long startPrecompiled = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            precompiled.matcher("STU123").matches();
        }
        long timePrecompiled = System.nanoTime() - startPrecompiled;

        System.out.printf("With recompilation: %,d ns%n", timeWithRecompilation);
        System.out.printf("Precompiled: %,d ns%n", timePrecompiled);
        System.out.printf("Precompiled is %.1fx faster%n",
                (double) timeWithRecompilation / timePrecompiled);

        assertTrue(timePrecompiled < timeWithRecompilation,
                "Precompiled patterns should be faster");
    }

    @Test
    @DisplayName("Edge Cases and Special Characters")
    void testEdgeCases() {
        // Empty strings
        assertFalse(STUDENT_ID_PATTERN.matcher("").matches());
        assertFalse(EMAIL_PATTERN.matcher("").matches());

        // Whitespace
        assertFalse(STUDENT_ID_PATTERN.matcher(" STU123 ").matches());

        // Null handling (utilities should handle gracefully)
        var nullResult = ValidationUtils.validateStudentId(null);
        assertFalse(nullResult.isValid());
        assertNotNull(nullResult.getErrorMessage());

        // Very long strings
        String veryLong = "STU" + "1".repeat(1000);
        assertFalse(STUDENT_ID_PATTERN.matcher(veryLong).matches());

        // Unicode characters
        //assertFalse(NAME_PATTERN.matcher("José"));
        //assertFalse(EMAIL_PATTERN.matcher("josé@example.com"));
    }

    @Test
    @DisplayName("Regex Pattern Stress Test")
    void testRegexStressTest() {
        // Test with many variations
        List<String> testCases = new ArrayList<>();

        // Generate 1000 test cases
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            if (random.nextBoolean()) {
                testCases.add("STU" + String.format("%03d", random.nextInt(1000)));
            } else {
                testCases.add("ABC" + random.nextInt(1000));
            }
        }

        long start = System.nanoTime();
        int validCount = 0;

        for (String testCase : testCases) {
            if (STUDENT_ID_PATTERN.matcher(testCase).matches()) {
                validCount++;
            }
        }

        long time = System.nanoTime() - start;

        System.out.printf("Processed %,d regex matches in %,d ns (%.0f matches/sec)%n",
                testCases.size(), time, (testCases.size() * 1_000_000_000.0) / time);

        assertTrue(time < 100_000_000, "Should process 1000 matches quickly");
    }
}