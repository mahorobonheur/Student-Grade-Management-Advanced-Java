package org.example.application.service;

import org.example.domain.model.*;
import org.example.domain.repository.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;

public class CacheService {
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final StudentRepository studentRepo;
    private final GradeRepository gradeRepo;
    private int hits = 0;
    private int misses = 0;
    private static final int MAX_CACHE_SIZE = 150;
    Scanner scanner = new Scanner(System.in);

    private static class CacheEntry {
        Object data;
        LocalDateTime timestamp;
        long accessCount;

        CacheEntry(Object data) {
            this.data = data;
            this.timestamp = LocalDateTime.now();
            this.accessCount = 1;
        }
    }

    public CacheService(StudentRepository studentRepo, GradeRepository gradeRepo) {
        this.studentRepo = studentRepo;
        this.gradeRepo = gradeRepo;
        warmCache();
    }

    public Student getStudent(String studentId) {
        String key = "student_" + studentId;
        CacheEntry entry = cache.get(key);

        if (entry != null) {
            hits++;
            entry.accessCount++;
            entry.timestamp = LocalDateTime.now();
            return (Student) entry.data;
        } else {
            misses++;
            Student student = studentRepo.findById(studentId);
            if (student != null) {
                put(key, student);
            }
            return student;
        }
    }

    public List<Grade> getStudentGrades(String studentId) {
        String key = "grades_" + studentId;
        CacheEntry entry = cache.get(key);

        if (entry != null) {
            hits++;
            entry.accessCount++;
            entry.timestamp = LocalDateTime.now();
            return (List<Grade>) entry.data;
        } else {
            misses++;
            List<Grade> grades = gradeRepo.findByStudentId(studentId);
            if (!grades.isEmpty()) {
                put(key, grades);
            }
            return grades;
        }
    }

    public Double getStudentAverage(String studentId) {
        String key = "avg_" + studentId;
        CacheEntry entry = cache.get(key);

        if (entry != null) {
            hits++;
            entry.accessCount++;
            entry.timestamp = LocalDateTime.now();
            return (Double) entry.data;
        } else {
            misses++;
            Double average = gradeRepo.calculateStudentAverage(studentId);
            if (average > 0) {
                put(key, average);
            }
            return average;
        }
    }

    private void put(String key, Object data) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictLRU();
        }
        cache.put(key, new CacheEntry(data));
    }

    private void evictLRU() {
        String lruKey = null;
        long minAccess = Long.MAX_VALUE;
        LocalDateTime oldestTime = LocalDateTime.MAX;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            CacheEntry cacheEntry = entry.getValue();
            if (cacheEntry.accessCount < minAccess ||
                    (cacheEntry.accessCount == minAccess &&
                            cacheEntry.timestamp.isBefore(oldestTime))) {
                minAccess = cacheEntry.accessCount;
                oldestTime = cacheEntry.timestamp;
                lruKey = entry.getKey();
            }
        }

        if (lruKey != null) {
            cache.remove(lruKey);
        }
    }

    public void refreshCache() {
        // Refresh stale entries
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        cache.entrySet().removeIf(entry ->
                entry.getValue().timestamp.isBefore(cutoff));

        // Re-warm frequently accessed data
        warmCache();
    }

    private void warmCache() {
        // Cache top 20 students by access frequency
        studentRepo.findAll().stream()
                .limit(20)
                .forEach(student -> {
                    String key = "student_" + student.getStudentId();
                    cache.putIfAbsent(key, new CacheEntry(student));
                });
    }

    public void manageCache(Scanner scanner) {
        System.out.println("\n💾 CACHE MANAGEMENT CONSOLE");
        System.out.println("=".repeat(50));

        while (true) {
            System.out.println("\n1. View Cache Statistics");
            System.out.println("2. View Cache Contents");
            System.out.println("3. Clear Cache");
            System.out.println("4. Refresh Cache");
            System.out.println("5. Return to Main Menu");
            System.out.print("Select option: ");

            int choice = Integer.parseInt(scanner.nextLine().trim());

            switch (choice) {
                case 1:
                    displayStatistics();
                    break;
                case 2:
                    displayContents();
                    break;
                case 3:
                    clearCache();
                    break;
                case 4:
                    refreshCache();
                    System.out.println("✅ Cache refreshed!");
                    break;
                case 5:
                    return;
            }
        }
    }

    private void displayStatistics() {
        System.out.println("\nCACHE STATISTICS");
        System.out.println("=".repeat(40));
        System.out.printf("Cache Size: %d / %d%n", cache.size(), MAX_CACHE_SIZE);
        System.out.printf("Hit Rate: %.1f%%%n", getHitRate() * 100);
        System.out.printf("Hits: %d | Misses: %d%n", hits, misses);
        System.out.printf("Memory Usage: ~%.1f KB%n", estimateMemoryUsage() / 1024.0);

        // Calculate average access time
        long totalAccess = cache.values().stream()
                .mapToLong(entry -> entry.accessCount)
                .sum();
        System.out.printf("Average Accesses per Entry: %.1f%n",
                cache.isEmpty() ? 0 : totalAccess / (double) cache.size());
    }

    private void displayContents() {
        System.out.println("\nCACHE CONTENTS");
        System.out.println("=".repeat(80));
        System.out.printf("%-30s | %-20s | %-12s | %-10s%n",
                "Key", "Last Accessed", "Access Count", "Type");
        System.out.println("-".repeat(80));

        cache.forEach((key, entry) -> {
            String type = entry.data.getClass().getSimpleName();
            System.out.printf("%-30s | %-20s | %-12d | %-10s%n",
                    key.length() > 30 ? key.substring(0, 27) + "..." : key,
                    entry.timestamp.toString(),
                    entry.accessCount,
                    type);
        });
    }

    private void clearCache() {
        System.out.print("Are you sure you want to clear the cache? (Y/N): ");
        String confirm = scanner.nextLine().trim().toUpperCase();

        if (confirm.equals("Y")) {
            cache.clear();
            hits = 0;
            misses = 0;
            System.out.println("✅ Cache cleared!");
        }
    }

    public double getHitRate() {
        int total = hits + misses;
        return total == 0 ? 0 : (double) hits / total;
    }

    private long estimateMemoryUsage() {
        // Rough estimation: each entry ~100 bytes + data size
        return cache.size() * 100L + cache.values().stream()
                .mapToLong(entry -> estimateSize(entry.data))
                .sum();
    }

    private long estimateSize(Object obj) {
        if (obj instanceof Student) return 200;
        if (obj instanceof List) return ((List<?>) obj).size() * 100;
        if (obj instanceof Double) return 8;
        return 50;
    }
}