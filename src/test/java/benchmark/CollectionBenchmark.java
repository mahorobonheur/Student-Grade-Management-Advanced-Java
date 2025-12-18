package benchmark;

import org.example.domain.model.Student;
import org.example.domain.model.RegularStudent;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class CollectionBenchmark {

    @Param({"100", "1000", "10000"})
    private int size;

    private List<Student> arrayList;
    private Map<String, Student> hashMap;
    private TreeMap<String, Student> treeMap;
    private Set<Student> hashSet;

    @Setup
    public void setup() {
        arrayList = new ArrayList<>();
        hashMap = new HashMap<>();
        treeMap = new TreeMap<>();
        hashSet = new HashSet<>();

        for (int i = 0; i < size; i++) {
            String studentId = "STU" + String.format("%05d", i);
            Student student = new RegularStudent(
                    "Student" + i, 20, "test@school.edu", "555-1234"
            );
            student.setStudentId(studentId);

            arrayList.add(student);
            hashMap.put(studentId, student);
            treeMap.put(studentId, student);
            hashSet.add(student);
        }
    }

    @Benchmark
    public Student arrayListLookup() {
        // Sequential search (O(n))
        String targetId = "STU" + String.format("%05d", size / 2);
        for (Student s : arrayList) {
            if (s.getStudentId().equals(targetId)) {
                return s;
            }
        }
        return null;
    }

    @Benchmark
    public Student hashMapLookup() {
        // Hash lookup (O(1))
        String targetId = "STU" + String.format("%05d", size / 2);
        return hashMap.get(targetId);
    }

    @Benchmark
    public Student treeMapLookup() {
        // Tree lookup (O(log n))
        String targetId = "STU" + String.format("%05d", size / 2);
        return treeMap.get(targetId);
    }

    @Benchmark
    public boolean hashSetContains() {
        // Set contains (O(1))
        String targetId = "STU" + String.format("%05d", size / 2);
        Student target = new RegularStudent("Target", 20, "target@school.edu", "555-9999");
        target.setStudentId(targetId);
        return hashSet.contains(target);
    }

    @Benchmark
    public List<Student> arrayListIteration() {
        // Iterate through all elements
        List<Student> result = new ArrayList<>();
        for (Student s : arrayList) {
            if (s.getAge() > 18) {
                result.add(s);
            }
        }
        return result;
    }

    @Benchmark
    public Map<String, Student> hashMapIteration() {
        // Iterate through map entries
        Map<String, Student> result = new HashMap<>();
        for (Map.Entry<String, Student> entry : hashMap.entrySet()) {
            if (entry.getValue().getAge() > 18) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CollectionBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}