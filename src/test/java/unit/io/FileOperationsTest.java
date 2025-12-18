package unit.io;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.io.*;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;
import java.util.zip.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("File Operations Tests")
class FileOperationsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("NIO.2 File Reading with Various Sizes")
    void testNIOFileReading() throws IOException {
        // Create test files of different sizes
        Path smallFile = tempDir.resolve("small.txt");
        Path mediumFile = tempDir.resolve("medium.txt");
        Path largeFile = tempDir.resolve("large.txt");

        // Small file (1KB)
        String smallContent = "A".repeat(1024);
        Files.writeString(smallFile, smallContent);

        // Medium file (100KB)
        String mediumContent = "B".repeat(1024 * 100);
        Files.writeString(mediumFile, mediumContent);

        // Large file (10MB)
        String largeContent = "C".repeat(1024 * 1024 * 10);
        Files.write(largeFile, largeContent.getBytes(StandardCharsets.UTF_8));

        // Test reading small file
        long smallStart = System.nanoTime();
        String smallRead = Files.readString(smallFile);
        long smallTime = System.nanoTime() - smallStart;
        assertEquals(smallContent.length(), smallRead.length());

        // Test reading medium file
        long mediumStart = System.nanoTime();
        String mediumRead = Files.readString(mediumFile);
        long mediumTime = System.nanoTime() - mediumStart;
        assertEquals(mediumContent.length(), mediumRead.length());

        // Test reading large file with streaming
        long largeStart = System.nanoTime();
        byte[] largeBytes = Files.readAllBytes(largeFile);
        long largeTime = System.nanoTime() - largeStart;
        assertEquals(largeContent.length(), largeBytes.length);

        System.out.printf("Small file (1KB): %,d ns%n", smallTime);
        System.out.printf("Medium file (100KB): %,d ns%n", mediumTime);
        System.out.printf("Large file (10MB): %,d ns%n", largeTime);

        // Verify files exist and are readable
        assertTrue(Files.exists(smallFile));
        assertTrue(Files.isReadable(smallFile));
        assertTrue(Files.size(smallFile) > 0);
    }

    @Test
    @DisplayName("Streaming vs Full File Load Memory Usage")
    void testStreamingVsFullLoad() throws IOException {
        // Create a large CSV file
        Path csvFile = tempDir.resolve("data.csv");

        // Generate 100,000 lines of data
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile)) {
            for (int i = 0; i < 100_000; i++) {
                writer.write(String.format("Student%d,%d,%d,%d%n",
                        i, i % 100, i % 50 + 50, i % 20 + 80));
            }
        }

        long fileSize = Files.size(csvFile);
        System.out.printf("Test file size: %,d bytes%n", fileSize);

        // Method 1: Full file load (readAllLines)
        Runtime runtime = Runtime.getRuntime();

        System.gc();
        long memoryBeforeFull = runtime.totalMemory() - runtime.freeMemory();

        long fullLoadStart = System.nanoTime();
        List<String> allLines = Files.readAllLines(csvFile);
        long fullLoadTime = System.nanoTime() - fullLoadStart;

        long memoryAfterFull = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsedFull = memoryAfterFull - memoryBeforeFull;

        System.out.printf("Full load: %,d ns, Memory: %,d bytes%n",
                fullLoadTime, memoryUsedFull);

        // Method 2: Streaming (Files.lines)
        System.gc();
        long memoryBeforeStream = runtime.totalMemory() - runtime.freeMemory();

        long streamStart = System.nanoTime();
        long lineCount;
        try (Stream<String> lines = Files.lines(csvFile)) {
            lineCount = lines.count();
        }
        long streamTime = System.nanoTime() - streamStart;

        long memoryAfterStream = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsedStream = memoryAfterStream - memoryBeforeStream;

        System.out.printf("Streaming: %,d ns, Memory: %,d bytes%n",
                streamTime, memoryUsedStream);

        // Verify both methods read same data
        assertEquals(allLines.size(), lineCount);

        // Streaming should use less memory for large files
        assertTrue(memoryUsedStream < memoryUsedFull * 1.5,
                "Streaming should use comparable or less memory");
    }

    @Test
    @DisplayName("Concurrent File Access Handling")
    void testConcurrentFileAccess() throws IOException, InterruptedException {
        Path sharedFile = tempDir.resolve("shared.txt");
        Files.writeString(sharedFile, "Initial content\n");

        int threadCount = 5;
        int writesPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Threads will append to file concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < writesPerThread; j++) {
                        // Use synchronized or Files.write with APPEND and CREATE options
                        String content = String.format("Thread%d-Write%d%n", threadId, j);

                        // Method 1: Synchronized block (slower but safe)
                        synchronized (this) {
                            Files.writeString(sharedFile, content,
                                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                        }

                        // Small delay to increase chance of interleaving
                        Thread.sleep(1);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Wait for all threads
        assertTrue(latch.await(10, TimeUnit.SECONDS));

        // Verify file was written
        long lineCount = Files.lines(sharedFile).count();
        assertEquals(threadCount * writesPerThread + 1, lineCount); // +1 for initial line

        // Verify no data corruption (all lines should end with newline and have expected format)
        List<String> lines = Files.readAllLines(sharedFile);
        for (String line : lines.subList(1, lines.size())) { // Skip initial line
            assertTrue(line.matches("Thread\\d+-Write\\d+"));
        }
    }

    @Test
    @DisplayName("UTF-8 File Encoding Handling")
    void testUTF8Encoding() throws IOException {
        // Test various Unicode characters
        String testContent =
                "ASCII: Hello World\n" +
                        "Latin: Café naïve\n" +
                        "Cyrillic: Привет мир\n" +
                        "Chinese: 你好世界\n" +
                        "Emoji: 😀🎉🚀\n" +
                        "Special: ©®™€£¥\n";

        Path utf8File = tempDir.resolve("utf8-test.txt");

        // Write with UTF-8
        Files.writeString(utf8File, testContent, StandardCharsets.UTF_8);

        // Read back with UTF-8
        String readContent = Files.readString(utf8File, StandardCharsets.UTF_8);

        assertEquals(testContent, readContent, "UTF-8 content should be preserved");

        // Test with different encodings (should fail)
        Path latin1File = tempDir.resolve("latin1-test.txt");
        assertThrows(CharacterCodingException.class, () -> {
            // Try to write UTF-8 content as Latin-1
            Files.writeString(latin1File, testContent, StandardCharsets.ISO_8859_1);
        });

        // Test BOM handling
        byte[] bom = {(byte)0xEF, (byte)0xBB, (byte)0xBF}; // UTF-8 BOM
        Path bomFile = tempDir.resolve("bom-test.txt");

        try (OutputStream os = Files.newOutputStream(bomFile)) {
            os.write(bom);
            os.write(testContent.getBytes(StandardCharsets.UTF_8));
        }

        // Read with BOM
        String withBom = Files.readString(bomFile, StandardCharsets.UTF_8);
        assertTrue(withBom.startsWith("\uFEFF") || withBom.equals(testContent),
                "BOM should be handled or stripped");
    }

    @Test
    @DisplayName("Mock File System for Testing")
    void testMockFileSystem() throws IOException {
        // Create in-memory file system
        FileSystem fs = FileSystems.newFileSystem(
                URI.create("memory://"),
                Collections.emptyMap()
        );

        Path memoryPath = fs.getPath("/test.txt");

        // Write to memory file system
        Files.writeString(memoryPath, "Test content in memory");

        // Read back
        String content = Files.readString(memoryPath);
        assertEquals("Test content in memory", content);

        // List files
        Path root = fs.getPath("/");
        try (Stream<Path> stream = Files.list(root)) {
            List<Path> files = stream.collect(Collectors.toList());
            assertEquals(1, files.size());
        }

        // Delete file
        Files.delete(memoryPath);
        assertFalse(Files.exists(memoryPath));

        fs.close();
    }

    @Test
    @DisplayName("File Operation Exception Scenarios")
    void testFileOperationExceptions() throws IOException {
        // Non-existent file
        Path nonExistent = tempDir.resolve("nonexistent.txt");
        assertThrows(NoSuchFileException.class, () -> {
            Files.readAllLines(nonExistent);
        });

        // Directory instead of file
        assertThrows(AccessDeniedException.class, () -> {
            Files.readAllLines(tempDir);
        });

        // Permission denied (simulated by creating read-only file)
        Path readOnlyFile = tempDir.resolve("readonly.txt");
        Files.writeString(readOnlyFile, "read only");
        assertTrue(readOnlyFile.toFile().setReadOnly());

        assertThrows(AccessDeniedException.class, () -> {
            Files.writeString(readOnlyFile, "try to write");
        });

        // Clean up: need to set writable to delete
        readOnlyFile.toFile().setWritable(true);

        // Invalid path characters
        assertThrows(InvalidPathException.class, () -> {
            Path invalid = tempDir.resolve("test\0null.txt");
            Files.createFile(invalid);
        });

        // File too large (simulate by trying to create huge file)
        // Note: We'll create a reasonable size for testing
        Path largeFile = tempDir.resolve("large.bin");
        byte[] data = new byte[1024 * 1024]; // 1MB

        // This should work
        assertDoesNotThrow(() -> {
            Files.write(largeFile, data);
        });

        // Test disk full scenario (simulated by quota)
        // Can't easily simulate without actual disk manipulation
        System.out.println("Note: Disk full scenario requires special setup");
    }

    @Test
    @DisplayName("Temporary File Cleanup")
    void testTemporaryFileCleanup() throws IOException {
        // Create temporary files
        Path tempFile1 = Files.createTempFile(tempDir, "test", ".tmp");
        Path tempFile2 = Files.createTempFile(tempDir, "test", ".tmp");

        assertTrue(Files.exists(tempFile1));
        assertTrue(Files.exists(tempFile2));

        // Write some data
        Files.writeString(tempFile1, "Temporary data 1");
        Files.writeString(tempFile2, "Temporary data 2");

        // Verify data
        assertEquals("Temporary data 1", Files.readString(tempFile1));

        // Delete files
        Files.delete(tempFile1);
        Files.delete(tempFile2);

        assertFalse(Files.exists(tempFile1));
        assertFalse(Files.exists(tempFile2));

        // Test deleteIfExists
        Path anotherTemp = Files.createTempFile(tempDir, "another", ".tmp");
        assertTrue(Files.deleteIfExists(anotherTemp));
        assertFalse(Files.deleteIfExists(anotherTemp)); // Already deleted

        // Test with directories
        Path tempDir2 = Files.createTempDirectory(tempDir, "subdir");
        Path fileInDir = tempDir2.resolve("file.txt");
        Files.writeString(fileInDir, "File in temp dir");

        // Delete directory with contents
        Files.walk(tempDir2)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore for test
                    }
                });

        assertFalse(Files.exists(tempDir2));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 100, 1000, 10000})
    @DisplayName("File I/O Performance with Different Sizes")
    void testFileIOPerformance(int lineCount) throws IOException {
        Path testFile = tempDir.resolve("performance-" + lineCount + ".txt");

        // Generate file with specified number of lines
        long writeStart = System.nanoTime();
        try (BufferedWriter writer = Files.newBufferedWriter(testFile)) {
            for (int i = 0; i < lineCount; i++) {
                writer.write(String.format("Line %08d: This is test data for performance measurement.%n", i));
            }
        }
        long writeTime = System.nanoTime() - writeStart;

        long fileSize = Files.size(testFile);

        // Read file
        long readStart = System.nanoTime();
        List<String> lines = Files.readAllLines(testFile);
        long readTime = System.nanoTime() - readStart;

        // Read with streaming
        long streamStart = System.nanoTime();
        long streamCount;
        try (Stream<String> stream = Files.lines(testFile)) {
            streamCount = stream.count();
        }
        long streamTime = System.nanoTime() - streamStart;

        System.out.printf("Lines: %,6d, Size: %,8d bytes, Write: %,8d ns, Read: %,8d ns, Stream: %,8d ns%n",
                lineCount, fileSize, writeTime, readTime, streamTime);

        assertEquals(lineCount, lines.size());
        assertEquals(lineCount, streamCount);

        // Performance expectations
        if (lineCount > 1000) {
            assertTrue(writeTime < 10_000_000_000L, "Write should complete within 10 seconds");
            assertTrue(readTime < 10_000_000_000L, "Read should complete within 10 seconds");
        }
    }

    @Test
    @DisplayName("File Locking Mechanisms")
    void testFileLocking() throws IOException, InterruptedException {
        Path lockedFile = tempDir.resolve("locked.txt");
        Files.writeString(lockedFile, "Initial content");

        AtomicBoolean lockAcquired = new AtomicBoolean(false);
        AtomicBoolean lockBlocked = new AtomicBoolean(false);

        // Thread 1: Acquire lock
        Thread lockThread = new Thread(() -> {
            try (FileChannel channel = FileChannel.open(lockedFile,
                    StandardOpenOption.READ, StandardOpenOption.WRITE);
                 FileLock lock = channel.lock()) {

                lockAcquired.set(true);

                // Hold lock for a while
                Thread.sleep(2000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Thread 2: Try to access locked file
        Thread accessThread = new Thread(() -> {
            try {
                // Wait for lock to be acquired
                Thread.sleep(500);

                // Try to get lock (should block or fail)
                try (FileChannel channel = FileChannel.open(lockedFile,
                        StandardOpenOption.READ, StandardOpenOption.WRITE)) {

                    // Try lock (non-blocking)
                    FileLock tryLock = channel.tryLock();
                    if (tryLock == null) {
                        lockBlocked.set(true);
                        System.out.println("Lock blocked - file is locked by another thread");
                    } else {
                        tryLock.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        lockThread.start();
        accessThread.start();

        lockThread.join();
        accessThread.join();

        assertTrue(lockAcquired.get());
        assertTrue(lockBlocked.get(), "Second thread should be blocked from getting lock");

        // Test shared locks
        Path sharedFile = tempDir.resolve("shared-lock.txt");
        Files.writeString(sharedFile, "Shared content");

        // Multiple threads can have shared locks
        CountDownLatch sharedLatch = new CountDownLatch(3);
        AtomicInteger sharedLockCount = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try (FileChannel channel = FileChannel.open(sharedFile,
                        StandardOpenOption.READ);
                     FileLock lock = channel.lock(0, Long.MAX_VALUE, true)) { // Shared lock

                    sharedLockCount.incrementAndGet();
                    Thread.sleep(100);

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    sharedLatch.countDown();
                }
            }).start();
        }

        sharedLatch.await();
        assertEquals(3, sharedLockCount.get(), "All threads should acquire shared locks");
    }
}