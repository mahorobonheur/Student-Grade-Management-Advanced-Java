package unit.concurrent;

import org.example.application.service.CacheService;
import org.example.domain.model.Student;
import org.example.domain.model.RegularStudent;
import org.example.domain.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Concurrency Tests")
@Execution(ExecutionMode.CONCURRENT)
class ConcurrencyTest {

    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 1000;

    private ExecutorService executor;
    private StudentRepository studentRepo;
    private GradeRepository gradeRepo;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(THREAD_COUNT);
        studentRepo = new InMemoryStudentRepository();
        gradeRepo = new InMemoryGradeRepository();
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Thread-Safe Collections Under Concurrent Load")
    @Timeout(10)
    void testThreadSafeCollections() throws InterruptedException {
        // Use concurrent collections
        ConcurrentMap<String, AtomicInteger> concurrentMap = new ConcurrentHashMap<>();
        List<String> synchronizedList = Collections.synchronizedList(new ArrayList<>());
        ConcurrentLinkedQueue<String> concurrentQueue = new ConcurrentLinkedQueue<>();

        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // Submit tasks to executor
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String key = "key-" + threadId + "-" + j;
                        String value = "value-" + threadId + "-" + j;

                        // Concurrent map operations
                        concurrentMap.compute(key, (k, v) -> {
                            if (v == null) return new AtomicInteger(1);
                            v.incrementAndGet();
                            return v;
                        });

                        // Synchronized list operations
                        synchronizedList.add(value);

                        // Concurrent queue operations
                        concurrentQueue.offer(value);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS),
                "All threads should complete within timeout");

        // Verify data consistency
        assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, synchronizedList.size());
        assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, concurrentQueue.size());

        // Verify no duplicate keys in map (each thread uses unique keys)
        assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, concurrentMap.size());

        // All values should be 1 (each key unique)
        for (AtomicInteger count : concurrentMap.values()) {
            assertEquals(1, count.get());
        }
    }

    @Test
    @DisplayName("Race Conditions in Unsynchronized Code")
    @Timeout(10)
    void testRaceConditions() throws InterruptedException {
        // Non-thread-safe counter
        class UnsafeCounter {
            private int count = 0;

            public void increment() {
                count++;  // Not atomic - race condition!
            }

            public int getCount() {
                return count;
            }
        }

        // Thread-safe counter for comparison
        class SafeCounter {
            private final AtomicInteger count = new AtomicInteger(0);

            public void increment() {
                count.incrementAndGet();
            }

            public int getCount() {
                return count.get();
            }
        }

        UnsafeCounter unsafeCounter = new UnsafeCounter();
        SafeCounter safeCounter = new SafeCounter();

        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // Submit tasks that increment counters
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    unsafeCounter.increment();
                    safeCounter.increment();
                }
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        int totalOperations = THREAD_COUNT * OPERATIONS_PER_THREAD;

        // Safe counter should have correct value
        assertEquals(totalOperations, safeCounter.getCount(),
                "Thread-safe counter should have correct count");

        // Unsafe counter likely has incorrect value due to race conditions
        System.out.printf("Unsafe counter: %,d (expected: %,d)%n",
                unsafeCounter.getCount(), totalOperations);

        // In concurrent environment, unsafe counter often has less than expected
        // due to lost updates
        assertNotEquals(totalOperations, unsafeCounter.getCount(),
                "Unsafe counter likely has race condition");
    }

    @Test
    @DisplayName("Deadlock Scenarios and Prevention")
    @Timeout(10)
    void testDeadlockPrevention() throws InterruptedException {
        // Resources that could cause deadlock
        Object resourceA = new Object();
        Object resourceB = new Object();

        AtomicBoolean deadlockDetected = new AtomicBoolean(false);
        CountDownLatch completed = new CountDownLatch(2);

        // Thread 1: locks A then B (potential deadlock)
        Thread thread1 = new Thread(() -> {
            synchronized (resourceA) {
                try {
                    Thread.sleep(100);  // Ensure thread2 locks B
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                synchronized (resourceB) {
                    System.out.println("Thread 1 acquired both locks");
                }
            }
            completed.countDown();
        });

        // Thread 2: locks B then A (reverse order - deadlock!)
        Thread thread2 = new Thread(() -> {
            synchronized (resourceB) {
                try {
                    Thread.sleep(100);  // Ensure thread1 locks A
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // This will deadlock!
                synchronized (resourceA) {
                    System.out.println("Thread 2 acquired both locks");
                }
            }
            completed.countDown();
        });

        // Start both threads
        thread1.start();
        thread2.start();

        // Wait with timeout - if deadlock occurs, this will timeout
        boolean noDeadlock = completed.await(2, TimeUnit.SECONDS);

        if (!noDeadlock) {
            deadlockDetected.set(true);
            thread1.interrupt();
            thread2.interrupt();
        }

        // Test with deadlock prevention (consistent lock ordering)
        System.out.println("Testing deadlock prevention...");

        CountDownLatch preventionCompleted = new CountDownLatch(2);
        Object resource1 = new Object();
        Object resource2 = new Object();

        // Both threads always lock in same order
        Thread thread3 = new Thread(() -> {
            synchronized (resource1) {
                synchronized (resource2) {
                    System.out.println("Thread 3 acquired locks in order");
                }
            }
            preventionCompleted.countDown();
        });

        Thread thread4 = new Thread(() -> {
            synchronized (resource1) {
                synchronized (resource2) {
                    System.out.println("Thread 4 acquired locks in order");
                }
            }
            preventionCompleted.countDown();
        });

        thread3.start();
        thread4.start();

        assertTrue(preventionCompleted.await(2, TimeUnit.SECONDS),
                "Deadlock prevention should allow completion");
    }

    @Test
    @DisplayName("ExecutorService Shutdown Handling")
    @Timeout(10)
    void testExecutorServiceShutdown() throws InterruptedException {
        // Test normal shutdown
        ExecutorService testExecutor = Executors.newFixedThreadPool(2);

        AtomicInteger completedTasks = new AtomicInteger(0);
        CountDownLatch taskStarted = new CountDownLatch(5);

        // Submit some tasks
        for (int i = 0; i < 5; i++) {
            testExecutor.submit(() -> {
                taskStarted.countDown();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                completedTasks.incrementAndGet();
            });
        }

        // Wait for all tasks to start
        assertTrue(taskStarted.await(2, TimeUnit.SECONDS));

        // Shutdown gracefully
        testExecutor.shutdown();

        // Wait for termination
        boolean terminated = testExecutor.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(terminated, "Executor should terminate gracefully");
        assertEquals(5, completedTasks.get());

        // Test shutdownNow()
        ExecutorService interruptingExecutor = Executors.newFixedThreadPool(2);
        AtomicInteger interruptedCount = new AtomicInteger(0);

        // Submit long-running tasks
        for (int i = 0; i < 5; i++) {
            interruptingExecutor.submit(() -> {
                try {
                    Thread.sleep(10_000);  // Long sleep
                } catch (InterruptedException e) {
                    interruptedCount.incrementAndGet();
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(100);  // Let tasks start

        // Force shutdown
        List<Runnable> pendingTasks = interruptingExecutor.shutdownNow();
        assertFalse(pendingTasks.isEmpty(), "Should have pending tasks");

        terminated = interruptingExecutor.awaitTermination(2, TimeUnit.SECONDS);
        assertTrue(terminated, "Executor should terminate after shutdownNow");

        // Some tasks should have been interrupted
        assertTrue(interruptedCount.get() > 0);
    }

    @Test
    @DisplayName("Cache Consistency Under Concurrent Access")
    @Timeout(10)
    void testCacheConsistency() throws InterruptedException {
        // Create cache service
        CacheService cacheService = new CacheService(studentRepo, gradeRepo);

        // Create test student
        Student testStudent = new RegularStudent("Test Student", 20,
                "test@school.edu", "555-1234");
        testStudent.setStudentId("STU999");
        studentRepo.save(testStudent);

        // Simulate concurrent cache access
        int readerThreads = 5;
        int writerThreads = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(readerThreads + writerThreads);

        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);
        AtomicBoolean consistencyViolated = new AtomicBoolean(false);

        // Reader threads
        for (int i = 0; i < readerThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 100; j++) {
                        Student student = cacheService.getStudent("STU999");
                        if (student == null) {
                            consistencyViolated.set(true);
                        }
                        readCount.incrementAndGet();
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Writer threads (update student)
        for (int i = 0; i < writerThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 10; j++) {
                        // Update student
                        testStudent.setName("Updated " + threadId + "-" + j);
                        studentRepo.update(testStudent);

                        // Clear cache
                        cacheService.refreshCache();
                        writeCount.incrementAndGet();
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads
        startLatch.countDown();

        // Wait for completion
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        System.out.printf("Cache test: %,d reads, %,d writes%n",
                readCount.get(), writeCount.get());

        assertFalse(consistencyViolated.get(),
                "Cache should maintain consistency under concurrent access");

        // Verify final state
        Student finalStudent = cacheService.getStudent("STU999");
        assertNotNull(finalStudent);
        assertTrue(finalStudent.getName().startsWith("Updated"));
    }

    @Test
    @DisplayName("Concurrent Modification Exception Handling")
    @Timeout(5)
    void testConcurrentModification() {
        // Test with non-concurrent collection
        List<Integer> list = new ArrayList<>();
        IntStream.range(0, 1000).forEach(list::add);

        // This should throw ConcurrentModificationException
        assertThrows(ConcurrentModificationException.class, () -> {
            for (Integer num : list) {
                if (num % 2 == 0) {
                    list.remove(num);  // Modification during iteration
                }
            }
        });

        // Test with concurrent collection (should not throw)
        ConcurrentLinkedQueue<Integer> concurrentQueue = new ConcurrentLinkedQueue<>();
        IntStream.range(0, 1000).forEach(concurrentQueue::add);

        // Safe to modify during iteration
        assertDoesNotThrow(() -> {
            for (Integer num : concurrentQueue) {
                if (num % 2 == 0) {
                    concurrentQueue.remove(num);
                }
            }
        });

        // Test with CopyOnWriteArrayList
        List<Integer> copyOnWriteList = new CopyOnWriteArrayList<>();
        IntStream.range(0, 1000).forEach(copyOnWriteList::add);

        assertDoesNotThrow(() -> {
            for (Integer num : copyOnWriteList) {
                if (num % 2 == 0) {
                    copyOnWriteList.remove(num);
                }
            }
        });
    }

    @Test
    @DisplayName("Thread Pool Performance Under Load")
    @Timeout(30)
    void testThreadPoolPerformance() throws InterruptedException {
        int taskCount = 1000;
        CountDownLatch latch = new CountDownLatch(taskCount);

        long startTime = System.currentTimeMillis();

        // Submit tasks
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // Simulate work
                    Thread.sleep(10);
                    // Do some computation
                    Math.sqrt(taskId * Math.PI);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all tasks
        assertTrue(latch.await(15, TimeUnit.SECONDS));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.printf("Completed %,d tasks in %,d ms with %,d threads%n",
                taskCount, duration, THREAD_COUNT);

        // Should be faster than sequential (1000 * 10ms = 10 seconds)
        assertTrue(duration < 5000,
                "Thread pool should process tasks faster than sequential");

        // Calculate throughput
        double throughput = (double) taskCount / (duration / 1000.0);
        System.out.printf("Throughput: %.1f tasks/second%n", throughput);

        assertTrue(throughput > 50, "Should achieve reasonable throughput");
    }

    @Test
    @DisplayName("Atomic Operations and Memory Visibility")
    void testAtomicOperations() {
        // Test various atomic classes
        AtomicInteger atomicInt = new AtomicInteger(0);
        AtomicLong atomicLong = new AtomicLong(0);
        AtomicReference<String> atomicRef = new AtomicReference<>("initial");
        AtomicBoolean atomicBool = new AtomicBoolean(false);

        // Test atomic increment
        int threads = 10;
        int increments = 1000;

        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < increments; j++) {
                    atomicInt.incrementAndGet();
                    atomicLong.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertEquals(threads * increments, atomicInt.get());
        assertEquals(threads * increments, atomicLong.get());

        // Test compare-and-swap
        boolean updated = atomicRef.compareAndSet("initial", "updated");
        assertTrue(updated);
        assertEquals("updated", atomicRef.get());

        // Test atomic boolean
        assertTrue(atomicBool.compareAndSet(false, true));
        assertTrue(atomicBool.get());
    }
}