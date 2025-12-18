/*package grade;

import org.example.grade.Grade;
import org.example.grade.GradeManager;
import org.example.newImprementations.FileExporter;
import org.example.service.BulkImportService;
import org.example.student.RegularStudent;
import org.example.student.Student;
import org.example.student.StudentManager;
import org.example.subject.CoreSubject;
import org.example.subject.ElectiveSubject;
import org.example.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GradeManagerTest {

    @Mock
    private StudentManager studentManager;

    @Mock
    private BulkImportService bulkImportService;

    private GradeManager gradeManager;
    private RegularStudent testStudent;
    private CoreSubject mathSubject;
    private ElectiveSubject musicSubject;
    private AutoCloseable closeable;
    private MockedStatic<FileExporter> mockedFileExporter;

    @BeforeEach
    void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);

        gradeManager = new GradeManager(studentManager);

        mockedFileExporter = Mockito.mockStatic(FileExporter.class);

        setPrivateField(gradeManager, "bulkImportService", bulkImportService);


        Grade[] gradesArray = new Grade[200];
        setPrivateField(gradeManager, "grades", gradesArray);
        setPrivateField(gradeManager, "gradeCount", 0);

        // Initialize subjects array
        Subject[] subjects = new Subject[] {
                new CoreSubject("Mathematics", "MAT101"),
                new CoreSubject("English", "EN101"),
                new CoreSubject("Science", "SC101"),
                new ElectiveSubject("Music", "MU101"),
                new ElectiveSubject("Art", "AR101"),
                new ElectiveSubject("Physical Education", "PE101")
        };
        setPrivateField(gradeManager, "subject", subjects);

        // Create test data
        testStudent = new RegularStudent("MAHORO Bonheur", 20, "bonheurmahoro@example.com", "0782817801");
        testStudent.setStudentId("STU001");

        mathSubject = new CoreSubject("Mathematics", "MAT101");
        musicSubject = new ElectiveSubject("Music", "MU101");
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
        if (mockedFileExporter != null) {
            mockedFileExporter.close();
        }
    }

    @Test
    void testCalculateOverallAverage_NoGrades() {
        double average = gradeManager.calculateOverallAverage("STU001");

        assertEquals(0.0, average, 0.01);
    }

    @Test
    void testCalculateOverallAverage_WithGrades() throws Exception {

        Grade grade1 = new Grade("STU001", mathSubject, 85.0);
        Grade grade2 = new Grade("STU001", musicSubject, 90.0);

        setGradesArray(new Grade[]{grade1, grade2});
        setGradeCount(2);

        double average = gradeManager.calculateOverallAverage("STU001");


        assertEquals(87.5, average, 0.01);
    }

    @Test
    void testCalculateCoreAverage() throws Exception {

        CoreSubject english = new CoreSubject("English", "EN101");
        Grade coreGrade1 = new Grade("STU001", mathSubject, 80.0);
        Grade coreGrade2 = new Grade("STU001", english, 90.0);
        Grade electiveGrade = new Grade("STU001", musicSubject, 85.0);

        setGradesArray(new Grade[]{coreGrade1, coreGrade2, electiveGrade});
        setGradeCount(3);

        double coreAverage = gradeManager.calculateCoreAverage("STU001");


        assertEquals(85.0, coreAverage, 0.01);
    }

    @Test
    void testCalculateElectiveAverage() throws Exception {

        ElectiveSubject art = new ElectiveSubject("Art", "AR101");
        Grade coreGrade = new Grade("STU001", mathSubject, 80.0);
        Grade electiveGrade1 = new Grade("STU001", musicSubject, 90.0);
        Grade electiveGrade2 = new Grade("STU001", art, 85.0);

        setGradesArray(new Grade[]{coreGrade, electiveGrade1, electiveGrade2});
        setGradeCount(3);

        double electiveAverage = gradeManager.calculateElectiveAverage("STU001");

        assertEquals(87.5, electiveAverage, 0.01);
    }

    @Test
    void testGetRegisteredSubjects() throws Exception {

        CoreSubject english = new CoreSubject("English", "EN101");
        Grade grade1 = new Grade("STU001", mathSubject, 85.0);
        Grade grade2 = new Grade("STU001", english, 90.0);
        Grade grade3 = new Grade("STU001", mathSubject, 88.0);

        setGradesArray(new Grade[]{grade1, grade2, grade3});
        setGradeCount(3);

        int subjectCount = gradeManager.getRegisteredSubjects("STU001");

        assertEquals(2, subjectCount);
    }

    @Test
    void testGetGradeCount() throws Exception {

        Grade grade1 = new Grade("STU001", mathSubject, 85.0);
        Grade grade2 = new Grade("STU001", musicSubject, 90.0);

        setGradesArray(new Grade[]{grade1, grade2});
        setGradeCount(2);

        int gradeCount = gradeManager.getGradeCount();

        assertEquals(2, gradeCount);
    }

    @Test
    void testFindExistingGrade() throws Exception {

        Grade existingGrade = new Grade("STU001", mathSubject, 85.0);
        existingGrade.setGradeId("GRD001");

        setGradesArray(new Grade[]{existingGrade});
        setGradeCount(1);


        Grade found = findExistingGrade("STU001", "MAT101");

        assertNotNull(found);
        assertEquals("GRD001", found.getGradeId());
        assertEquals(85.0, found.getGrade(), 0.01);
    }

    @Test
    void testFindExistingGrade_NotFound() throws Exception {

        Grade existingGrade = new Grade("STU001", mathSubject, 85.0);

        setGradesArray(new Grade[]{existingGrade});
        setGradeCount(1);


        Grade found = findExistingGrade("STU001", "EN101");


        assertNull(found);
    }

    @Test
    void testAddGrade_NewGrade() throws Exception {

        String input = "STU001\n1\n1\n85\nY\n\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        setScanner(testScanner);

        when(studentManager.findStudent("STU001")).thenReturn(testStudent);

        mockedFileExporter.when(() -> FileExporter.updateStudentGradeFile(
                        any(Student.class), any(Grade.class), anyDouble(), anyInt()))
                .thenAnswer(invocation -> null);

        doNothing().when(bulkImportService).saveOrUpdateGradeCSV(any(Student.class), any(Grade.class));


        gradeManager.addGrade(null);

        verify(studentManager, times(2)).findStudent("STU001");
    }

    @Test
    void testAddGrade_UpdateExistingGrade() throws Exception {

        Grade existingGrade = new Grade("STU001", mathSubject, 75.0);
        existingGrade.setGradeId("GRD001");

        setGradesArray(new Grade[]{existingGrade});
        setGradeCount(1);

        String input = "STU001\n1\n1\n90\nY\n\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        setScanner(testScanner);

        when(studentManager.findStudent("STU001")).thenReturn(testStudent);

        mockedFileExporter.when(() -> FileExporter.updateStudentGradeFile(
                        any(Student.class), any(Grade.class), anyDouble(), anyInt()))
                .thenAnswer(invocation -> null);


        gradeManager.addGrade(null);

        assertEquals(90.0, existingGrade.getGrade(), 0.01);
    }

    @Test
    void testAddGrade_StudentNotFound() throws Exception {
        String input = "STU999\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        setScanner(testScanner);

        when(studentManager.findStudent("STU999")).thenReturn(null);

        gradeManager.addGrade(null);

        verify(studentManager, times(1)).findStudent("STU999");
    }

    @Test
    void testAddGrade_CancelConfirmation() throws Exception {
        String input = "STU001\n1\n1\n85\nN\n\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner testScanner = new Scanner(inputStream);
        setScanner(testScanner);

        when(studentManager.findStudent("STU001")).thenReturn(testStudent);


        gradeManager.addGrade(null);
   }

    @Test
    void testGetGradesByStudentId() throws Exception {

        Grade grade1 = new Grade("STU001", mathSubject, 85.0);
        Grade grade2 = new Grade("STU001", musicSubject, 90.0);
        Grade grade3 = new Grade("STU002", mathSubject, 75.0);

        setGradesArray(new Grade[]{grade1, grade2, grade3});
        setGradeCount(3);


        List<Grade> studentGrades = getGradesByStudentId("STU001");

        assertNotNull(studentGrades);
        assertEquals(2, studentGrades.size());
        assertEquals(85.0, studentGrades.get(0).getGrade(), 0.01);
        assertEquals(90.0, studentGrades.get(1).getGrade(), 0.01);
    }

    @Test
    void testCalculateClassRank() throws Exception {

        RegularStudent student1 = new RegularStudent("Student 1", 20, "s1@example.com", "1111111111");
        student1.setStudentId("STU001");

        RegularStudent student2 = new RegularStudent("Student 2", 21, "s2@example.com", "2222222222");
        student2.setStudentId("STU002");

        RegularStudent student3 = new RegularStudent("Student 3", 22, "s3@example.com", "3333333333");
        student3.setStudentId("STU003");

        Student[] students = {student1, student2, student3};

        when(studentManager.getAllStudents()).thenReturn(students);
        when(studentManager.getStudentCount()).thenReturn(3);

        Grade grade1 = new Grade("STU001", mathSubject, 95.0);
        Grade grade2 = new Grade("STU002", mathSubject, 85.0);
        Grade grade3 = new Grade("STU003", mathSubject, 90.0);

        setGradesArray(new Grade[]{grade1, grade2, grade3});
        setGradeCount(3);

        double avg1 = gradeManager.calculateOverallAverage("STU001");
        double avg2 = gradeManager.calculateOverallAverage("STU002");
        double avg3 = gradeManager.calculateOverallAverage("STU003");

        int rank1 = calculateClassRank("STU001");
        int rank2 = calculateClassRank("STU002");
        int rank3 = calculateClassRank("STU003");

        assertEquals(1, rank1);
        assertEquals(3, rank2);
        assertEquals(2, rank3);
    }

    @Test
    void testCalculateMode() throws Exception {

        double[] numbers = {85.0, 90.0, 85.0, 88.0, 90.0, 90.0, 92.0};

        double mode = calculateMode(numbers);

        assertEquals(90.0, mode, 0.01);
    }

    @Test
    void testCalculateMode_EmptyArray() throws Exception {

        double[] numbers = {};

        double mode = calculateMode(numbers);

        assertEquals(0.0, mode, 0.01);
    }

    @Test
    void testCalculateStandardDeviation() throws Exception {

        double mean = 85.0;
        int count = 3;

        Grade grade1 = new Grade("STU001", mathSubject, 80.0);
        Grade grade2 = new Grade("STU002", mathSubject, 85.0);
        Grade grade3 = new Grade("STU003", mathSubject, 90.0);

        setGradesArray(new Grade[]{grade1, grade2, grade3});
        setGradeCount(3);

        double stdDev = calculateStandardDeviation(mean, count);

        assertEquals(4.082, stdDev, 0.01);
    }

    @Test
    void testGradeInitialization() {

        Grade grade = new Grade("STU001", mathSubject, 85.0);
        grade.setGradeId("GRD001");

        assertEquals("GRD001", grade.getGradeId());
        assertEquals("STU001", grade.getStudentId());
        assertEquals(mathSubject, grade.getSubject());
        assertEquals(85.0, grade.getGrade(), 0.01);
        assertNotNull(grade.getDate());
    }

    @Test
    void testGradeDisplayDetails() {

        Grade grade = new Grade("STU001", mathSubject, 85.0);
        grade.setGradeId("GRD001");

        assertDoesNotThrow(() -> grade.displayGradeDetails());
    }

    @Test
    void testGradeSetters() {

        Grade grade = new Grade("STU001", mathSubject, 85.0);

        grade.setGradeId("GRD999");
        assertEquals("GRD999", grade.getGradeId());

        grade.setGrade(95.0);
        assertEquals(95.0, grade.getGrade(), 0.01);

        grade.setStudentId("STU999");
        assertEquals("STU999", grade.getStudentId());

        ElectiveSubject newSubject = new ElectiveSubject("Art", "AR101");
        grade.setSubject(newSubject);
        assertEquals(newSubject, grade.getSubject());
    }

    @Test
    void testSubjectInheritance() {

        CoreSubject core = new CoreSubject("Mathematics", "MAT101");
        ElectiveSubject elective = new ElectiveSubject("Music", "MU101");

        assertTrue(core instanceof org.example.subject.Subject);
        assertTrue(elective instanceof org.example.subject.Subject);

        assertEquals("Core", core.getSubjectType());
        assertEquals("Elective", elective.getSubjectType());

        assertEquals("Mathematics", core.getSubjectName());
        assertEquals("MAT101", core.getSubjectCode());

        assertEquals("Music", elective.getSubjectName());
        assertEquals("MU101", elective.getSubjectCode());
    }

    @Test
    void testSubjectEquality() {

        CoreSubject math1 = new CoreSubject("Mathematics", "MAT101");
        CoreSubject math2 = new CoreSubject("Mathematics", "MAT101");
        CoreSubject english = new CoreSubject("English", "EN101");

        assertEquals(math1.getSubjectCode(), math2.getSubjectCode());
        assertNotEquals(math1.getSubjectCode(), english.getSubjectCode());
    }

    @Test
    void testGradeToString() {

        Grade grade = new Grade("STU001", mathSubject, 85.0);
        grade.setGradeId("GRD001");

        String toString = grade.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("GRD001") || toString.contains("STU001"));
    }

    @Test
    void testGradeDate() {

        Grade grade = new Grade("STU001", mathSubject, 85.0);

        String date = grade.getDate();
        assertNotNull(date);
        assertEquals(10, date.length());
    }

    @Test
    void testCalculateOverallAverage_NoMatchingStudent() throws Exception {
        Grade grade1 = new Grade("STU001", mathSubject, 85.0);
        Grade grade2 = new Grade("STU002", musicSubject, 90.0);

        setGradesArray(new Grade[]{grade1, grade2});
        setGradeCount(2);

        double average = gradeManager.calculateOverallAverage("STU999");

        assertEquals(0.0, average, 0.01);
    }

    @Test
    void testCalculateCoreAverage_NoCoreGrades() throws Exception {

        Grade electiveGrade1 = new Grade("STU001", musicSubject, 90.0);
        ElectiveSubject art = new ElectiveSubject("Art", "AR101");
        Grade electiveGrade2 = new Grade("STU001", art, 85.0);

        setGradesArray(new Grade[]{electiveGrade1, electiveGrade2});
        setGradeCount(2);


        double coreAverage = gradeManager.calculateCoreAverage("STU001");

        assertEquals(0.0, coreAverage, 0.01);
    }

    @Test
    void testCalculateElectiveAverage_NoElectiveGrades() throws Exception {
        Grade coreGrade1 = new Grade("STU001", mathSubject, 80.0);
        CoreSubject english = new CoreSubject("English", "EN101");
        Grade coreGrade2 = new Grade("STU001", english, 90.0);

        setGradesArray(new Grade[]{coreGrade1, coreGrade2});
        setGradeCount(2);


        double electiveAverage = gradeManager.calculateElectiveAverage("STU001");

        assertEquals(0.0, electiveAverage, 0.01);
    }

    @Test
    void testGetRegisteredSubjects_NoGrades() {

        int subjectCount = gradeManager.getRegisteredSubjects("STU001");

        assertEquals(0, subjectCount);
    }

    @Test
    void testGradeComparisons() {

        Grade grade1 = new Grade("STU001", mathSubject, 85.0);
        Grade grade2 = new Grade("STU001", mathSubject, 90.0);

        assertNotEquals(grade1, grade2);
        assertEquals(grade1.getStudentId(), grade2.getStudentId());
    }



    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void setGradesArray(Grade[] grades) throws Exception {
        setPrivateField(gradeManager, "grades", grades);
    }

    private void setGradeCount(int count) throws Exception {
        setPrivateField(gradeManager, "gradeCount", count);
    }

    private void setScanner(Scanner scanner) throws Exception {
        setPrivateField(gradeManager, "scanner", scanner);
    }

    private Grade findExistingGrade(String studentId, String subjectCode) throws Exception {
        Method method = GradeManager.class.getDeclaredMethod("findExistingGrade", String.class, String.class);
        method.setAccessible(true);
        return (Grade) method.invoke(gradeManager, studentId, subjectCode);
    }

    private List<Grade> getGradesByStudentId(String studentId) throws Exception {
        Method method = GradeManager.class.getDeclaredMethod("getGradesByStudentId", String.class);
        method.setAccessible(true);
        return (List<Grade>) method.invoke(gradeManager, studentId);
    }

    private int calculateClassRank(String studentId) throws Exception {
        Method method = GradeManager.class.getDeclaredMethod("calculateClassRank", String.class);
        method.setAccessible(true);
        return (int) method.invoke(gradeManager, studentId);
    }

    private double calculateMode(double[] numbers) throws Exception {
        Method method = GradeManager.class.getDeclaredMethod("calculateMode", double[].class);
        method.setAccessible(true);
        return (double) method.invoke(gradeManager, (Object) numbers);
    }

    private double calculateStandardDeviation(double mean, int count) throws Exception {
        Method method = GradeManager.class.getDeclaredMethod("calculateStandardDeviation", double.class, int.class);
        method.setAccessible(true);
        return (double) method.invoke(gradeManager, mean, count);
    }
}*/