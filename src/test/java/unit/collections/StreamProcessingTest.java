package unit.collections;

import org.example.domain.model.Student;
import org.example.domain.model.RegularStudent;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Stream Processing Tests")
class StreamProcessingTest {

    private List<Student> students;
    private List<Integer> numbers;

    @BeforeEach
    void setUp() {
        students = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Student student = new RegularStudent(
                    "Student" + i,
                    15 + (i % 10),
                    "student" + i + "@school.edu",
                    "555-000" + String.format("%03d", i)
            );
            student.setStudentId("STU" + String.format("%03d", i));
            student.setStatus(i % 5 == 0 ? "INACTIVE" : "ACTIVE");
            students.add(student);
        }

        numbers = IntStream.rangeClosed(1, 1000)
                .boxed()
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("Filter, Map, Reduce Operations")
    void testFilterMapReduce() {
        // Filter active students, map to names, collect
        List<String> activeStudentNames = students.stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .map(Student::getName)
                .collect(Collectors.toList());

        assertEquals(80, activeStudentNames.size(),
                "Should have 80 active students");
        assertTrue(activeStudentNames.contains("Student1"));
        assertFalse(activeStudentNames.contains("Student5")); // Inactive

        // Reduce to find total age
        int totalAge = students.stream()
                .mapToInt(Student::getAge)
                .reduce(0, Integer::sum);

        int expectedTotalAge = students.stream()
                .mapToInt(Student::getAge)
                .sum();

        assertEquals(expectedTotalAge, totalAge);

        // Complex reduce with statistics
        IntSummaryStatistics ageStats = students.stream()
                .mapToInt(Student::getAge)
                .summaryStatistics();

        assertEquals(15, ageStats.getMin());
        assertEquals(24, ageStats.getMax());
        assertEquals(100, ageStats.getCount());
    }

    @Test
    @DisplayName("Parallel Stream Correctness")
    void testParallelStreamCorrectness() {
        // Sequential result
        List<String> sequentialNames = students.stream()
                .filter(s -> s.getAge() >= 18)
                .map(Student::getName)
                .sorted()
                .collect(Collectors.toList());

        // Parallel result
        List<String> parallelNames = students.parallelStream()
                .filter(s -> s.getAge() >= 18)
                .map(Student::getName)
                .sorted()
                .collect(Collectors.toList());

        // Results should be identical
        assertEquals(sequentialNames.size(), parallelNames.size());
        assertEquals(sequentialNames, parallelNames);

        // Test with stateful operations
        List<Integer> sequentialSquares = numbers.stream()
                .map(n -> n * n)
                .collect(Collectors.toList());

        List<Integer> parallelSquares = numbers.parallelStream()
                .map(n -> n * n)
                .collect(Collectors.toList());

        assertEquals(sequentialSquares, parallelSquares);
    }

    @Test
    @DisplayName("Stream Short-Circuiting Operations")
    void testShortCircuiting() {
        // findFirst should short-circuit
        Optional<Student> firstTeen = students.stream()
                .filter(s -> s.getAge() < 18)
                .findFirst();

        assertTrue(firstTeen.isPresent());
        assertEquals(15, firstTeen.get().getAge());

        // limit should short-circuit
        List<Student> firstFive = students.stream()
                .limit(5)
                .collect(Collectors.toList());

        assertEquals(5, firstFive.size());
        assertEquals("Student1", firstFive.get(0).getName());

        // anyMatch should short-circuit
        boolean hasMinor = students.stream()
                .anyMatch(s -> s.getAge() < 18);

        assertTrue(hasMinor);

        // allMatch may short-circuit on first false
        boolean allAdults = students.stream()
                .allMatch(s -> s.getAge() >= 18);

        assertFalse(allAdults);
    }

    @Test
    @DisplayName("Lazy Evaluation Behavior")
    void testLazyEvaluation() {
        // Counter to track evaluations
        AtomicInteger evaluationCount = new AtomicInteger(0);

        Stream<String> lazyStream = students.stream()
                .peek(s -> evaluationCount.incrementAndGet())
                .filter(s -> s.getAge() > 20)
                .map(Student::getName)
                .limit(3);

        // No terminal operation yet - should not evaluate
        assertEquals(0, evaluationCount.get());

        // Trigger evaluation with terminal operation
        List<String> result = lazyStream.collect(Collectors.toList());

        // Should only evaluate until 3 matches found
        assertTrue(evaluationCount.get() < 30,
                "Should evaluate lazily, not all 100 students");
        assertEquals(3, result.size());

        // Test with infinite stream
        Stream<Integer> infinite = Stream.iterate(1, n -> n + 1)
                .peek(n -> {
                    if (n > 10) throw new IllegalStateException("Should stop at 10");
                })
                .filter(n -> n % 2 == 0)
                .limit(5);

        List<Integer> firstFiveEvens = infinite.collect(Collectors.toList());
        assertEquals(Arrays.asList(2, 4, 6, 8, 10), firstFiveEvens);
    }

    @Test
    @DisplayName("Grouping and Partitioning")
    void testGroupingAndPartitioning() {
        // Group by age
        Map<Integer, List<Student>> studentsByAge = students.stream()
                .collect(Collectors.groupingBy(Student::getAge));

        // Each age group should contain correct students
        studentsByAge.forEach((age, studentList) -> {
            assertTrue(studentList.stream()
                    .allMatch(s -> s.getAge() == age));
        });

        // Partition by adult/minor
        Map<Boolean, List<Student>> partitioned = students.stream()
                .collect(Collectors.partitioningBy(s -> s.getAge() >= 18));

        assertTrue(partitioned.get(true).stream()
                .allMatch(s -> s.getAge() >= 18));
        assertTrue(partitioned.get(false).stream()
                .allMatch(s -> s.getAge() < 18));

        // Complex grouping with downstream collector
        Map<Integer, Long> countByAge = students.stream()
                .collect(Collectors.groupingBy(
                        Student::getAge,
                        Collectors.counting()
                ));

        long total = countByAge.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        assertEquals(100, total);
    }

    @Test
    @DisplayName("FlatMap and Nested Collections")
    void testFlatMap() {
        // Create students with multiple grades
        List<List<Integer>> studentGrades = Arrays.asList(
                Arrays.asList(85, 90, 78),
                Arrays.asList(92, 88, 95),
                Arrays.asList(76, 82, 80)
        );

        // Flatten all grades
        List<Integer> allGrades = studentGrades.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        assertEquals(9, allGrades.size());

        // Calculate average of all grades
        double average = allGrades.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        assertTrue(average > 0);
    }

    @RepeatedTest(3)
    @DisplayName("Sequential vs Parallel Performance Comparison")
    void testStreamPerformanceComparison() {
        List<Integer> largeList = IntStream.range(0, 1_000_000)
                .boxed()
                .collect(Collectors.toList());

        // Warm up
        largeList.stream().map(n -> n * 2).count();
        largeList.parallelStream().map(n -> n * 2).count();

        // Sequential processing
        long seqStart = System.nanoTime();
        long seqCount = largeList.stream()
                .filter(n -> n % 2 == 0)
                .map(n -> n * 3)
                .count();
        long seqTime = System.nanoTime() - seqStart;

        // Parallel processing
        long parStart = System.nanoTime();
        long parCount = largeList.parallelStream()
                .filter(n -> n % 2 == 0)
                .map(n -> n * 3)
                .count();
        long parTime = System.nanoTime() - parStart;

        assertEquals(seqCount, parCount);

        System.out.printf("Iteration - Sequential: %,d ns, Parallel: %,d ns, Ratio: %.2f%n",
                seqTime, parTime, (double) seqTime / parTime);
    }
}