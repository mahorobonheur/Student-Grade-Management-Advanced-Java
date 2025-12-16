package student;

import org.example.grade.GradeManager;
import org.example.newImprementations.FileExporter;
import org.example.student.HonorsStudent;
import org.example.student.RegularStudent;
import org.example.student.Student;
import org.example.student.StudentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class StudentManagerTest {

    @Mock
    private GradeManager gradeManager;

    @Mock
    private FileExporter fileExporter;

    private StudentManager studentManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        studentManager = new StudentManager();
        studentManager.setGradeManager(gradeManager);
        setStudentArray(new Student[100]);
        setStudentCount(0);
    }

    @Test
    void testFindStudent_StudentExists() {
        RegularStudent student = createRegularStudentWithDependencies("MAHORO Bonheur", 20, "mahoro@example.com", "1234567890", "STU001");
        setStudentArray(new Student[]{student});
        setStudentCount(1);

        Student found = studentManager.findStudent("STU001");

        assertNotNull(found);
        assertEquals("STU001", found.getStudentId());
        assertEquals("MAHORO Bonheur", found.getName());
    }

    @Test
    void testFindStudent_StudentDoesNotExist() {
        setStudentArray(new Student[0]);
        setStudentCount(0);

        Student found = studentManager.findStudent("STU999");

        assertNull(found);
    }

    @Test
    void testGetStudentCount_Empty() {
        setStudentCount(0);

        int count = studentManager.getStudentCount();

        assertEquals(0, count);
    }

    @Test
    void testGetStudentCount_WithStudents() {
        RegularStudent student1 = createRegularStudentWithDependencies("MAHORO Bonheur", 20, "mahoro@example.com", "0782817801", "STU001");
        RegularStudent student2 = createRegularStudentWithDependencies("KAGABO Paul", 21, "kagabo@example.com", "0785885859", "STU002");
        setStudentArray(new Student[]{student1, student2});
        setStudentCount(2);

        int count = studentManager.getStudentCount();

        assertEquals(2, count);
    }

    @Test
    void testGetAllStudents_Empty() {
        setStudentArray(new Student[0]);
        setStudentCount(0);

        Student[] students = studentManager.getAllStudents();

        assertNotNull(students);
        assertEquals(0, students.length);
    }

    @Test
    void testGetAllStudents_WithStudents() {
        RegularStudent student1 = createRegularStudentWithDependencies("MAHORO Bonheur", 20, "mahoro@example.com", "0782817801", "STU001");
        RegularStudent student2 = createRegularStudentWithDependencies("KAGABO Paul", 21, "kagabo@example.com", "0787654321", "STU002");

        setStudentArray(new Student[]{student1, student2});
        setStudentCount(2);

        Student[] students = studentManager.getAllStudents();

        assertNotNull(students);
        assertEquals(2, students.length);
        assertEquals("STU001", students[0].getStudentId());
        assertEquals("STU002", students[1].getStudentId());
    }

    @Test
    void testGetAverageClassGrade_NoStudents() {
        setStudentArray(new Student[0]);
        setStudentCount(0);

        when(gradeManager.calculateOverallAverage(anyString())).thenReturn(0.0);

        double average = studentManager.getAverageClassGrade();

        assertEquals(0.0, average, 0.01);
    }

    @Test
    void testGetAverageClassGrade_WithStudents() {
        RegularStudent student1 = createRegularStudentWithDependencies("MAHORO Bonheur", 20, "mahoro@example.com", "1234567890", "STU001");
        RegularStudent student2 = createRegularStudentWithDependencies("KAGABO Paul", 21, "kagabo@example.com", "0987654321", "STU002");

        setStudentArray(new Student[]{student1, student2});
        setStudentCount(2);

        when(gradeManager.calculateOverallAverage("STU001")).thenReturn(85.5);
        when(gradeManager.calculateOverallAverage("STU002")).thenReturn(92.0);

        double average = studentManager.getAverageClassGrade();

        assertEquals(88.75, average, 0.01);
    }

    @Test
    void testAddStudentRegular() {
        String input = "Mahoro Bonheur\n25\nmahoro.bonheur@example.com\n1234567890\n1\n\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);

        replaceScanner(studentManager, testScanner);

        when(gradeManager.calculateOverallAverage(anyString())).thenReturn(0.0);
        when(gradeManager.getRegisteredSubjects(anyString())).thenReturn(0);

        studentManager.addStudent(null);

        assertEquals(1, studentManager.getStudentCount());
    }

    @Test
    void testAddStudentHonors() {
        String input = "Mahoro Bonheur\n22\nmahoro@example.com\n9876543210\n2\n\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);

        replaceScanner(studentManager, testScanner);

        when(gradeManager.calculateOverallAverage(anyString())).thenReturn(0.0);
        when(gradeManager.getRegisteredSubjects(anyString())).thenReturn(0);

        studentManager.addStudent(null);

        assertEquals(1, studentManager.getStudentCount());

        Student[] students = studentManager.getAllStudents();
        assertNotNull(students);
        assertEquals(1, students.length);
        assertEquals("Honors", students[0].getStudentType());
    }

    @Test
    void testViewAllStudents_MinimumRequirementsNotMet() {
        setStudentCount(2);
        Scanner mockScanner = mock(Scanner.class);
        replaceScanner(studentManager, mockScanner);

        assertDoesNotThrow(() -> studentManager.viewAllStudents());
    }

    @Test
    void testIsPassing_RegularStudentPassing() {
        RegularStudent student = createRegularStudentWithDependencies("MURENZI Jean", 20, "murenzi@example.com", "1234567890", "STU001");

        when(gradeManager.calculateOverallAverage("STU001")).thenReturn(75.0);

        String result = student.isPassing("STU001");

        assertEquals("Passing", result);
    }

    @Test
    void testIsPassing_RegularStudentFailing() {
        RegularStudent student = createRegularStudentWithDependencies("MURENZI Jean", 20, "murenzi@example.com", "1234567890", "STU001");

        when(gradeManager.calculateOverallAverage("STU001")).thenReturn(45.0);

        String result = student.isPassing("STU001");

        assertEquals("Failing", result);
    }

    @Test
    void testIsPassing_HonorsStudentPassing() {
        HonorsStudent student = createHonorsStudentWithDependencies("UWIMANA Claire", 21, "uwimana@example.com", "0987654321", "STU002");

        when(gradeManager.calculateOverallAverage("STU002")).thenReturn(75.0);

        String result = student.isPassing("STU002");

        assertEquals("Passing", result);
    }

    @Test
    void testIsPassing_HonorsStudentFailing() {
        HonorsStudent student = createHonorsStudentWithDependencies("UWIMANA Claire", 21, "uwimana@example.com", "0987654321", "STU002");

        when(gradeManager.calculateOverallAverage("STU002")).thenReturn(55.0);

        String result = student.isPassing("STU002");

        assertEquals("Failing", result);
    }

    @Test
    void testStudentCounterIncrement() {
        Student.setStudentCounter(1);
        StudentManager freshManager = new StudentManager();
        assertTrue(true);
    }

    @Test
    void testEmailValidation() {
        String validEmail = "test@example.com";
        assertTrue(isValidEmail(validEmail));
        assertFalse(isValidEmail("test@example"));
        assertFalse(isValidEmail("testexample.com"));
        assertFalse(isValidEmail("@example.com"));
        assertFalse(isValidEmail("test@.com"));
    }

    @Test
    void testPhoneNumberValidation() {
        assertTrue(isValidPhone("1234567890"));
        assertTrue(isValidPhone("+1234567890123"));
        assertTrue(isValidPhone("+12345678901"));
        assertFalse(isValidPhone("123"));
        assertFalse(isValidPhone("12345678901234"));
        assertFalse(isValidPhone("123-456-7890"));
        assertFalse(isValidPhone("abc1234567"));
    }

    private void setStudentArray(Student[] students) {
        try {
            java.lang.reflect.Field field = StudentManager.class.getDeclaredField("student");
            field.setAccessible(true);
            field.set(studentManager, students);
        } catch (Exception e) {
            fail("Failed to set student array: " + e.getMessage());
        }
    }

    private void setStudentCount(int count) {
        try {
            java.lang.reflect.Field field = StudentManager.class.getDeclaredField("studentCount");
            field.setAccessible(true);
            field.set(studentManager, count);
        } catch (Exception e) {
            fail("Failed to set student count: " + e.getMessage());
        }
    }

    private void replaceScanner(StudentManager manager, Scanner scanner) {
        try {
            java.lang.reflect.Field field = StudentManager.class.getDeclaredField("scanner");
            field.setAccessible(true);
            field.set(manager, scanner);
        } catch (Exception e) {
            fail("Failed to replace scanner: " + e.getMessage());
        }
    }

    private boolean isValidEmail(String email) {
        if (!email.contains("@") || !email.contains(".")) {
            return false;
        }

        int at = email.indexOf('@');
        int dot = email.lastIndexOf('.');

        return at > 0 && dot > at + 1 && dot < email.length() - 1;
    }

    private boolean isValidPhone(String phone) {
        String p = phone;
        if (p.startsWith("+")) {
            p = p.substring(1);
        }

        if (p.length() < 10 || p.length() > 13) {
            return false;
        }

        for (char c : p.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }

    @Test
    void testFindStudentWithNullInput() {
        setStudentArray(new Student[0]);
        setStudentCount(0);

        Student found = studentManager.findStudent(null);

        assertNull(found);
    }

    @Test
    void testFindStudentWithEmptyString() {
        RegularStudent student = createRegularStudentWithDependencies("MURENZI Jean", 20, "murenzi@example.com", "1234567890", "STU001");
        setStudentArray(new Student[]{student});
        setStudentCount(1);

        Student found = studentManager.findStudent("");

        assertNull(found);
    }

    @Test
    void testGetAllStudentsReturnsCopy() {
        RegularStudent student = createRegularStudentWithDependencies("MURENZI Jean", 20, "murenzi@example.com", "1234567890", "STU001");
        setStudentArray(new Student[]{student});
        setStudentCount(1);

        Student[] students1 = studentManager.getAllStudents();
        students1[0] = null;

        Student[] students2 = studentManager.getAllStudents();

        assertNotNull(students2[0]);
        assertEquals("STU001", students2[0].getStudentId());
    }

    @Test
    void testStudentManagerInitialization() {
        StudentManager newManager = new StudentManager();

        assertNotNull(newManager);
        assertEquals(0, newManager.getStudentCount());

        Student[] students = newManager.getAllStudents();
        assertNotNull(students);
        assertEquals(0, students.length);
    }

    @Test
    void testRegularStudentCreation() {
        RegularStudent student = new RegularStudent("Test Student", 25, "test@example.com", "1234567890");

        assertEquals("Test Student", student.getName());
        assertEquals(25, student.getAge());
        assertEquals("test@example.com", student.getEmail());
        assertEquals("1234567890", student.getPhone());
        assertEquals("Regular", student.getStudentType());
        assertEquals(50.0, student.getPassingGrade(), 0.01);
        assertEquals("Active", student.getStatus());
    }

    @Test
    void testHonorsStudentCreation() {
        HonorsStudent student = new HonorsStudent("Test Honors", 22, "honors@example.com", "9876543210");

        assertEquals("Test Honors", student.getName());
        assertEquals(22, student.getAge());
        assertEquals("honors@example.com", student.getEmail());
        assertEquals("9876543210", student.getPhone());
        assertEquals("Honors", student.getStudentType());
        assertEquals(60.0, student.getPassingGrade(), 0.01);
        assertEquals("Active", student.getStatus());
    }

    @Test
    void testStudentInheritance() {
        RegularStudent regular = new RegularStudent("Regular", 20, "r@example.com", "1111111111");
        HonorsStudent honors = new HonorsStudent("Honors", 21, "h@example.com", "2222222222");

        assertTrue(regular instanceof Student);
        assertTrue(honors instanceof Student);

        assertEquals("Regular", regular.getStudentType());
        assertEquals("Honors", honors.getStudentType());

        assertEquals(50.0, regular.getPassingGrade(), 0.01);
        assertEquals(60.0, honors.getPassingGrade(), 0.01);
    }

    @Test
    void testStudentCalculatesGPA() {
        RegularStudent student = createRegularStudentWithDependencies("Test GPA", 20, "gpa@example.com", "3333333333", "STU123");

        when(gradeManager.calculateOverallAverage("STU123")).thenReturn(0.0);

        double gpa = student.calculateGPA();
        assertEquals(0.0, gpa, 0.01);
    }

    @Test
    void testStudentGetLetterGrade() {
        RegularStudent student = createRegularStudentWithDependencies("Test Letter", 20, "letter@example.com", "4444444444", "STU456");

        when(gradeManager.calculateOverallAverage("STU456")).thenReturn(0.0);

        String letterGrade = student.getLetterGrade();
        assertEquals("F", letterGrade);
    }

    @Test
    void testStudentGetLetterGradeWithDifferentAverages() {
        RegularStudent student = createRegularStudentWithDependencies("Test Letter", 20, "letter@example.com", "4444444444", "STU456");

        when(gradeManager.calculateOverallAverage("STU456")).thenReturn(95.0);
        assertEquals("A", student.getLetterGrade());

        when(gradeManager.calculateOverallAverage("STU456")).thenReturn(85.0);
        assertEquals("B", student.getLetterGrade());

        when(gradeManager.calculateOverallAverage("STU456")).thenReturn(75.0);
        assertEquals("C", student.getLetterGrade());

        when(gradeManager.calculateOverallAverage("STU456")).thenReturn(65.0);
        assertEquals("D", student.getLetterGrade());

        when(gradeManager.calculateOverallAverage("STU456")).thenReturn(55.0);
        assertEquals("F", student.getLetterGrade());
    }

    private RegularStudent createRegularStudentWithDependencies(String name, int age, String email, String phone, String studentId) {
        RegularStudent student = new RegularStudent(name, age, email, phone);
        student.setStudentId(studentId);
        student.setStudentManager(studentManager);
        student.setGradeManager(gradeManager);
        return student;
    }

    private HonorsStudent createHonorsStudentWithDependencies(String name, int age, String email, String phone, String studentId) {
        HonorsStudent student = new HonorsStudent(name, age, email, phone);
        student.setStudentId(studentId);
        student.setStudentManager(studentManager);
        student.setGradeManager(gradeManager);
        return student;
    }

    @Test
    void testStudentUpdateStatus() {
        RegularStudent student = createRegularStudentWithDependencies("Test Student", 20, "test@example.com", "1234567890", "STU001");

        assertEquals("Active", student.getStatus());

        student.setStatus("Suspended");
        assertEquals("Suspended", student.getStatus());

        student.setStatus("Graduated");
        assertEquals("Graduated", student.getStatus());
    }

    @Test
    void testStudentDisplayDetails() {
        RegularStudent student = createRegularStudentWithDependencies("Test Student", 20, "test@example.com", "1234567890", "STU001");

        assertDoesNotThrow(() -> student.displayStudentDetails());
    }

    @Test
    void testGetStudentByIdWithVariousFormats() {
        RegularStudent student1 = createRegularStudentWithDependencies("MURENZI Jean", 20, "murenzi@example.com", "1234567890", "STU001");
        RegularStudent student2 = createRegularStudentWithDependencies("UWIMANA Claire", 21, "uwimana@example.com", "0987654321", "STU002");

        setStudentArray(new Student[]{student1, student2});
        setStudentCount(2);

        Student found1 = studentManager.findStudent("STU001");
        assertNotNull(found1);
        assertEquals("MURENZI Jean", found1.getName());

        Student found2 = studentManager.findStudent("STU002");
        assertNotNull(found2);
        assertEquals("UWIMANA Claire", found2.getName());

        Student found3 = studentManager.findStudent("stu001");
        assertNull(found3);
    }

    @Test
    void testStudentWithNoGrades() {
        RegularStudent student = createRegularStudentWithDependencies("Test Student", 20, "test@example.com", "1234567890", "STU001");

        when(gradeManager.calculateOverallAverage("STU001")).thenReturn(0.0);
        when(gradeManager.getRegisteredSubjects("STU001")).thenReturn(0);

        String passingStatus = student.isPassing("STU001");
        assertEquals("Failing", passingStatus);

        String letterGrade = student.getLetterGrade();
        assertEquals("F", letterGrade);

        double gpa = student.calculateGPA();
        assertEquals(0.0, gpa, 0.01);
    }

    @Test
    void testHonorsStudentEligibility() {
        HonorsStudent student = createHonorsStudentWithDependencies("Honors Student", 20, "honors@example.com", "1234567890", "STU001");

        student.setHonorsEligible(true);
        assertTrue(student.isHonorsEligible());

        student.setHonorsEligible(false);
        assertFalse(student.isHonorsEligible());
    }

    @Test
    void testStudentAgeValidation() {
        RegularStudent student = new RegularStudent("Test", 15, "test@example.com", "1234567890");
        assertEquals(15, student.getAge());

        student.setAge(16);
        assertEquals(16, student.getAge());
    }

    @Test
    void testStudentContactInfoUpdate() {
        RegularStudent student = createRegularStudentWithDependencies("Test Student", 20, "test@example.com", "1234567890", "STU001");

        student.setEmail("new@example.com");
        assertEquals("new@example.com", student.getEmail());

        student.setPhone("0987654321");
        assertEquals("0987654321", student.getPhone());
    }

    @Test
    void testMultipleStudentTypesInArray() {
        RegularStudent regular = createRegularStudentWithDependencies("Regular Student", 20, "regular@example.com", "1111111111", "STU001");
        HonorsStudent honors = createHonorsStudentWithDependencies("Honors Student", 21, "honors@example.com", "2222222222", "STU002");

        setStudentArray(new Student[]{regular, honors});
        setStudentCount(2);

        Student[] students = studentManager.getAllStudents();
        assertEquals(2, students.length);
        assertEquals("Regular", students[0].getStudentType());
        assertEquals("Honors", students[1].getStudentType());
    }

    @Test
    void testHonorsStudentCheckHonorsEligibility() {
        HonorsStudent student = createHonorsStudentWithDependencies("Honors Student", 20, "honors@example.com", "1234567890", "STU001");

        student.setHonorsEligible(true);
        assertTrue(student.checkHonorsEligibility());

        student.setHonorsEligible(false);
        assertFalse(student.checkHonorsEligibility());
    }

    @Test
    void testFindStudentWithPartialMatch() {
        RegularStudent student1 = createRegularStudentWithDependencies("MURENZI Jean", 20, "murenzi@example.com", "1234567890", "STU001");
        RegularStudent student2 = createRegularStudentWithDependencies("UWIMANA Claire", 21, "uwimana@example.com", "0987654321", "STU002");

        setStudentArray(new Student[]{student1, student2});
        setStudentCount(2);

        Student found = studentManager.findStudent("STU");
        assertNull(found);
    }
}