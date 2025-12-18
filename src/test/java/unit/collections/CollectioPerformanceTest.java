package unit.collections;

import org.example.domain.model.Student;
import org.example.domain.model.RegularStudent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests collection performance characteristics
 * Focus: HashMap, TreeMap, ArrayList, HashSet, Streams
 */
@DisplayName("Collection Performance Tests")
class CollectionPerformanceTest {

    private static final int SMALL_SIZE = 100;
    private static final int MEDIUM_SIZE = 10_000;
    private static final int LARGE_SIZE = 100_000;

    private List<Student> largeStudentList;
    private Map<String, Student> hashMap;
    private TreeMap<String, Student> treeMap;
    private Set<Student> hashSet;

    @BeforeEach
    void setUp() {
        // Setup test data
        largeStudentList = new ArrayList<>();
        hashMap = new HashMap<>();
        treeMap = new TreeMap<>();
        hashSet = new HashSet<>();

        for (int i = 0; i < LARGE_SIZE; i++) {
            String studentId = String.format("STU%03d", i);
            Student student = new RegularStudent(
                    "Student_" + i,
                    18 + (i % 10),
                    "student" + i + "@school.edu",
                    String.format("555-%04d", i)
            );
            student.setStudentId(studentId);

            largeStudentList.add(student);
            hashMap.put(studentId, student);
            treeMap.put(studentId, student);
            hashSet.add(student);
        }
    }

    @Test
    @DisplayName("HashMap vs ArrayList Lookup Performance")
    void testHashMapVsArrayListLookup() {
        // Test HashMap lookup
        long hashMapStart = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            String key = "STU500";
            hashMap.get(key);
        }
        long hashMapTime = System.nanoTime() - hashMapStart;

        // Test ArrayList sequential search
        long arrayListStart = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            String targetId = "STU500";
            for (Student s : largeStudentList) {
                if (s.getStudentId().equals(targetId)) {
                    break;
                }
            }
        }
        long arrayListTime = System.nanoTime() - arrayListStart;

        System.out.printf("HashMap lookup (10k): %,d ns%n", hashMapTime);
        System.out.printf("ArrayList search (10k): %,d ns%n", arrayListTime);
        System.out.printf("HashMap is %.1fx faster%n",
                (double) arrayListTime / hashMapTime);

        // HashMap should be significantly faster for large datasets
        assertTrue(hashMapTime < arrayListTime,
                "HashMap should be faster than ArrayList sequential search");
    }

    @Test
    @DisplayName("TreeMap Sorting Performance")
    void testTreeMapSortingPerformance() {
        // Create unsorted map
        Map<String, Student> unsortedMap = new HashMap<>();
        Random random = new Random();

        for (int i = 0; i < 10_000; i++) {
            int randomId = random.nextInt(100_000);
            String studentId = String.format("STU%05d", randomId);
            Student student = new RegularStudent(
                    "Student_" + randomId, 20, "test@school.edu", "555-1234"
            );
            student.setStudentId(studentId);
            unsortedMap.put(studentId, student);
        }

        // Measure TreeMap insertion (auto-sorting)
        long treeMapStart = System.nanoTime();
        TreeMap<String, Student> sortedTreeMap = new TreeMap<>(unsortedMap);
        long treeMapTime = System.nanoTime() - treeMapStart;

        // Measure manual sort
        long manualSortStart = System.nanoTime();
        List<Map.Entry<String, Student>> entryList = new ArrayList<>(unsortedMap.entrySet());
        entryList.sort(Map.Entry.comparingByKey());
        Map<String, Student> manuallySorted = new LinkedHashMap<>();
        for (Map.Entry<String, Student> entry : entryList) {
            manuallySorted.put(entry.getKey(), entry.getValue());
        }
        long manualSortTime = System.nanoTime() - manualSortStart;

        System.out.printf("TreeMap auto-sort: %,d ns%n", treeMapTime);
        System.out.printf("Manual sort: %,d ns%n", manualSortTime);

        // Verify TreeMap is sorted
        String previousKey = null;
        for (String key : sortedTreeMap.keySet()) {
            if (previousKey != null) {
                assertTrue(key.compareTo(previousKey) > 0,
                        "TreeMap should maintain sorted order");
            }
            previousKey = key;
        }
    }

    @Test
    @DisplayName("HashSet Uniqueness Guarantees")
    void testHashSetUniqueness() {
        Set<Student> testSet = new HashSet<>();
        Student duplicateStudent = new RegularStudent(
                "John Doe", 20, "john@school.edu", "555-1234"
        );
        duplicateStudent.setStudentId("STU001");

        // Add same student twice
        boolean firstAdd = testSet.add(duplicateStudent);
        boolean secondAdd = testSet.add(duplicateStudent);

        assertTrue(firstAdd, "First add should succeed");
        assertFalse(secondAdd, "Second add should fail (duplicate)");
        assertEquals(1, testSet.size(), "Set should contain only one element");

        // Test with different objects but same data
        Student sameDataStudent = new RegularStudent(
                "John Doe", 20, "john@school.edu", "555-1234"
        );
        sameDataStudent.setStudentId("STU001");

        // Since equals/hashCode not overridden, these are considered different
        boolean thirdAdd = testSet.add(sameDataStudent);
        assertTrue(thirdAdd, "Different object with same data should be added");
        assertEquals(2, testSet.size(), "Set should now contain two elements");
    }

    @Test
    @DisplayName("Big-O Complexity Empirical Test")
    void testBigOComplexity() {
        Map<Integer, Long> hashMapTimes = new TreeMap<>();
        Map<Integer, Long> arrayListTimes = new TreeMap<>();

        // Test different sizes
        int[] sizes = {100, 1_000, 10_000, 100_000};

        for (int size : sizes) {
            // Setup
            Map<String, String> testHashMap = new HashMap<>();
            List<String> testList = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                String value = "Value" + i;
                testHashMap.put("Key" + i, value);
                testList.add(value);
            }

            // Measure HashMap lookup
            long hashMapStart = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                testHashMap.get("Key" + (size / 2));
            }
            hashMapTimes.put(size, System.nanoTime() - hashMapStart);

            // Measure ArrayList search
            long listStart = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                testList.contains("Value" + (size / 2));
            }
            arrayListTimes.put(size, System.nanoTime() - listStart);
        }

        // Print results for analysis
        System.out.println("\nBig-O Complexity Analysis:");
        System.out.println("Size | HashMap O(1) | ArrayList O(n)");
        System.out.println("-".repeat(50));

        hashMapTimes.forEach((size, time) -> {
            System.out.printf("%6d | %,11d | %,14d%n",
                    size, time, arrayListTimes.get(size));
        });

        // Verify HashMap time grows slowly (O(1) characteristic)
        List<Integer> sizeList = new ArrayList<>(hashMapTimes.keySet());
        for (int i = 1; i < sizeList.size(); i++) {
            int prevSize = sizeList.get(i-1);
            int currSize = sizeList.get(i);
            long prevTime = hashMapTimes.get(prevSize);
            long currTime = hashMapTimes.get(currSize);

            // HashMap time should grow much slower than size increase
            double sizeRatio = (double) currSize / prevSize;
            double timeRatio = (double) currTime / prevTime;

            System.out.printf("Size increase: %.1fx, Time increase: %.2fx%n",
                    sizeRatio, timeRatio);

            // HashMap should show sub-linear growth
            assertTrue(timeRatio < sizeRatio * 2,
                    "HashMap should show O(1) or O(log n) characteristics");
        }
    }

    @Test
    @DisplayName("Sequential vs Parallel Stream Performance")
    void testStreamPerformance() {
        // Create a large list
        List<Integer> numbers = IntStream.range(0, 1_000_000)
                .boxed()
                .collect(Collectors.toList());

        // Sequential stream
        long seqStart = System.nanoTime();
        long seqSum = numbers.stream()
                .filter(n -> n % 2 == 0)
                .mapToLong(n -> n * 2L)
                .sum();
        long seqTime = System.nanoTime() - seqStart;

        // Parallel stream
        long parStart = System.nanoTime();
        long parSum = numbers.parallelStream()
                .filter(n -> n % 2 == 0)
                .mapToLong(n -> n * 2L)
                .sum();
        long parTime = System.nanoTime() - parStart;

        System.out.printf("Sequential stream: %,d ns%n", seqTime);
        System.out.printf("Parallel stream: %,d ns%n", parTime);
        System.out.printf("Speedup: %.2fx%n", (double) seqTime / parTime);

        // Verify results are equal
        assertEquals(seqSum, parSum, "Both streams should produce same result");

        // With enough cores, parallel should be faster for large datasets
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores > 4) {
            assertTrue(parTime < seqTime || parTime < seqTime * 1.5,
                    "Parallel stream should be faster or similar on multi-core systems");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10000, 100000})
    @DisplayName("Collection Scaling Tests")
    void testCollectionScaling(int size) {
        // Test HashMap scaling
        Map<String, String> map = new HashMap<>();
        long mapStart = System.nanoTime();
        for (int i = 0; i < size; i++) {
            map.put("key" + i, "value" + i);
        }
        long mapInsertTime = System.nanoTime() - mapStart;

        // Test ArrayList scaling
        List<String> list = new ArrayList<>();
        long listStart = System.nanoTime();
        for (int i = 0; i < size; i++) {
            list.add("value" + i);
        }
        long listInsertTime = System.nanoTime() - listStart;

        System.out.printf("Size %,d: HashMap insert=%,dns, ArrayList insert=%,dns%n",
                size, mapInsertTime, listInsertTime);

        // Both should complete within reasonable time
        assertTrue(mapInsertTime < 1_000_000_000L,
                "HashMap insertion should complete within 1 second");
        assertTrue(listInsertTime < 1_000_000_000L,
                "ArrayList insertion should complete within 1 second");
    }
}