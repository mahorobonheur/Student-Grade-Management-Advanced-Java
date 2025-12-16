package org.example.grade;

import org.example.newImprementations.FileExporter;
import org.example.newImprementations.GPACalculator;
import org.example.service.BulkImportService;
import org.example.student.*;
import org.example.subject.CoreSubject;
import org.example.subject.ElectiveSubject;
import org.example.subject.Subject;

import java.io.*;
import java.util.*;

public class GradeManager {
    // MAIN DATA STRUCTURES WITH BIG-O COMPLEXITIES:

    // TreeMap for sorted GPA rankings: O(log n) for put, get, remove
    // Key: GPA, Value: List of students with that GPA (descending order)
    private TreeMap<Double, List<Student>> gpaRankings = new TreeMap<>(Collections.reverseOrder());

    // HashSet for tracking unique courses: O(1) average for add, contains, remove
    private HashSet<String> uniqueCourses = new HashSet<>();

    // LinkedList for grade history (frequent insertions/deletions): O(1) for add/remove at ends
    private LinkedList<GradeHistoryEntry> gradeHistory = new LinkedList<>();

    // ArrayList for maintaining insertion order: O(1) amortized add, O(n) insert at index
    private ArrayList<Grade> gradeList = new ArrayList<>();

    // PriorityQueue for task scheduling: O(log n) insert/poll
    private PriorityQueue<GradeTask> taskQueue = new PriorityQueue<>(new GradeTaskComparator());

    // HashMap for quick student grade lookups: O(1) average for operations
    private Map<String, List<Grade>> studentGradesMap = new HashMap<>();

    private StudentManager studentManager;
    private BulkImportService bulkImportService = new BulkImportService();
    private Scanner scanner = new Scanner(System.in);

    private Subject[] subjects = {
            new CoreSubject("Mathematics", "MAT101"),
            new CoreSubject("English", "ENG101"),
            new CoreSubject("Science", "SCI101"),
            new ElectiveSubject("Music", "MUS101"),
            new ElectiveSubject("Art", "ART101"),
            new ElectiveSubject("Physical Education", "PE101")
    };

    // CUSTOM COMPARATOR CLASSES:

    // Comparator for sorting students by multiple criteria
    public class StudentMultiComparator implements Comparator<Student> {
        @Override
        public int compare(Student s1, Student s2) {
            // 1. By student type (Honors first) - O(1)
            int typeCompare = s2.getStudentType().compareTo(s1.getStudentType());
            if (typeCompare != 0) return typeCompare;

            // 2. By GPA (descending) - O(1) if GPA is cached
            double gpa1 = calculateOverallAverage(s1.getStudentId());
            double gpa2 = calculateOverallAverage(s2.getStudentId());
            int gpaCompare = Double.compare(gpa2, gpa1);
            if (gpaCompare != 0) return gpaCompare;

            // 3. By number of subjects (descending) - O(1) if cached
            int subjects1 = getRegisteredSubjects(s1.getStudentId());
            int subjects2 = getRegisteredSubjects(s2.getStudentId());
            int subjectCompare = Integer.compare(subjects2, subjects1);
            if (subjectCompare != 0) return subjectCompare;

            // 4. By name (ascending) - O(k) where k is length
            return s1.getName().compareToIgnoreCase(s2.getName());
        }
    }

    // Comparator for grade tasks (higher priority first)
    public class GradeTaskComparator implements Comparator<GradeTask> {
        @Override
        public int compare(GradeTask t1, GradeTask t2) {
            // Higher priority number = higher priority - O(1)
            return Integer.compare(t2.getPriority(), t1.getPriority());
        }
    }

    // DATA STRUCTURE HELPER CLASSES:

    // Grade history entry for LinkedList
    public class GradeHistoryEntry {
        private String studentId;
        private String subjectCode;
        private double oldGrade;
        private double newGrade;
        private Date timestamp;
        private String action; // "ADD", "UPDATE", "DELETE"

        public GradeHistoryEntry(String studentId, String subjectCode,
                                 double oldGrade, double newGrade, String action) {
            this.studentId = studentId;
            this.subjectCode = subjectCode;
            this.oldGrade = oldGrade;
            this.newGrade = newGrade;
            this.action = action;
            this.timestamp = new Date();
        }


        public String getStudentId() { return studentId; }
        public String getSubjectCode() { return subjectCode; }
        public double getOldGrade() { return oldGrade; }
        public double getNewGrade() { return newGrade; }
        public String getAction() { return action; }
        public Date getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("[%s] %s - %s: %.1f → %.1f",
                    timestamp, studentId, subjectCode, oldGrade, newGrade);
        }
    }

    // Grade task for PriorityQueue
    public class GradeTask {
        private String description;
        private int priority; // 1=low, 5=high
        private Date scheduledTime;
        private Runnable taskAction;

        public GradeTask(String description, int priority, Runnable taskAction) {
            this.description = description;
            this.priority = priority;
            this.taskAction = taskAction;
            this.scheduledTime = new Date();
        }


        public String getDescription() { return description; }
        public int getPriority() { return priority; }
        public Date getScheduledTime() { return scheduledTime; }
        public Runnable getTaskAction() { return taskAction; }

        @Override
        public String toString() {
            return String.format("[P%d] %s (Scheduled: %s)",
                    priority, description, scheduledTime);
        }
    }

    public GradeManager() {
        for (Subject subject : subjects) {
            if (subject != null) {
                uniqueCourses.add(subject.getSubjectCode()); // O(1) average
            }
        }
    }

    public GradeManager(StudentManager studentManager) {
        this();
        this.studentManager = studentManager;
    }

    // MAIN GRADE OPERATIONS:

    public void addGrade(Grade theGrade) {
        // Add task to queue - O(log n) insertion
        taskQueue.add(new GradeTask("Process new grade", 3, () -> {
            System.out.println("Processing grade task...");
        }));

        System.out.println("RECORD GRADE");
        System.out.println("__________________________");
        System.out.print("Enter Student ID: ");
        String id = scanner.nextLine().trim();

        // O(1) average lookup in studentManager's HashMap
        Student student = studentManager.findStudent(id);
        if (student == null) {
            System.out.println("Student with ID " + id + " Not found!");
            return;
        }

        // Track unique courses for this student - O(1) average
        trackStudentCourses(id);

        System.out.println("Student Details:");
        System.out.println("Name: " + student.getName());
        System.out.println("Type: " + student.getStudentType());
        System.out.printf("Current Average: %.1f%% " , calculateOverallAverage(id));
        System.out.println();

        System.out.println("Subject Type:");
        System.out.println("1. Core Subject (Mathematics, English, Science)");
        System.out.println("2. Elective Subject (Music, Arts, Physical Education)");
        System.out.print("Select Type (1–2): ");

        int choice;
        while (true) {
            try {
                choice = Integer.parseInt(scanner.nextLine());
                if (choice == 1 || choice == 2) break;
            } catch (Exception ignored) { }
            System.out.print("Invalid! Enter 1 or 2: ");
        }

        // Using ArrayList for subject filtering - O(n) where n is subjects count
        List<Subject> filtered = new ArrayList<>();
        for (Subject s : subjects) {
            if (choice == 1 && s.getSubjectType().equals("Core")) {
                filtered.add(s); // O(1) amortized
            }
            else if (choice == 2 && s.getSubjectType().equals("Elective")) {
                filtered.add(s); // O(1) amortized
            }
        }

        System.out.println(choice == 1 ? "Available Core Subjects:" : "Available Elective Subjects:");
        for (int i = 0; i < filtered.size(); i++) {
            System.out.println((i + 1) + ". " + filtered.get(i).getSubjectName());
        }

        System.out.print("Select Subject (1–" + filtered.size() + "): ");
        int subjectChoice;

        while (true) {
            try {
                subjectChoice = Integer.parseInt(scanner.nextLine());
                if (subjectChoice >= 1 && subjectChoice <= filtered.size()) break;
            } catch (Exception ignored) { }
            System.out.print("Invalid! Choose between 1–" + filtered.size() + ": ");
        }

        Subject selectedSubject = filtered.get(subjectChoice - 1);

        uniqueCourses.add(selectedSubject.getSubjectCode());

        System.out.print("Enter grade (0–100): ");
        double gradeValue;

        while (true) {
            try {
                gradeValue = Double.parseDouble(scanner.nextLine());
                if (gradeValue >= 0 && gradeValue <= 100) break;
            } catch (Exception ignored) { }
            System.out.print("Invalid! Enter grade between 0–100: ");
        }

        // Check existing grade using HashMap - O(1) average
        Grade existing = findExistingGrade(id, selectedSubject.getSubjectCode());

        if (existing != null) {
            System.out.println("\nA grade for this subject already exists!");
            System.out.println("Current Marks: " + existing.getGrade() + "%");

            System.out.print("Do you want to UPDATE the marks? (Y/N): ");
            String update = scanner.nextLine().trim();

            while (!update.equalsIgnoreCase("Y") && !update.equalsIgnoreCase("N")) {
                System.out.print("Invalid! Enter Y or N: ");
                update = scanner.nextLine().trim();
            }

            if (update.equalsIgnoreCase("Y")) {
                gradeHistory.addFirst(new GradeHistoryEntry(
                        id, selectedSubject.getSubjectCode(),
                        existing.getGrade(), gradeValue, "UPDATE"
                ));

                existing.setGrade(gradeValue);
                System.out.println("Grade updated successfully!");

                // Update GPA rankings - O(log n)
                updateStudentGpaRanking(student);

                double newAverage = calculateOverallAverage(id);
                int totalSubjects = getRegisteredSubjects(id);
                FileExporter.updateStudentGradeFile(student, existing, newAverage, totalSubjects);

            } else {
                System.out.println("Update cancelled.");
            }

            System.out.println("Press enter to continue.");
            scanner.nextLine();
            return;
        }

        theGrade = new Grade(id, selectedSubject, gradeValue);
        theGrade.setGradeId(String.format("GRD%03d", gradeList.size() + 1));

        gradeList.add(theGrade);

        // Add to student's grade list in HashMap - O(1) average
        studentGradesMap.computeIfAbsent(id, k -> new ArrayList<>()).add(theGrade);

        // Add to grade history (LinkedList) - O(1) at beginning
        gradeHistory.addFirst(new GradeHistoryEntry(
                id, selectedSubject.getSubjectCode(),
                0, gradeValue, "ADD"
        ));


        updateStudentGpaRanking(student);

        // Add to unique grades TreeSet - O(log n)
        updateUniqueGrades(gradeValue);

        System.out.println("\nGRADE CONFIRMATION");
        System.out.println("_________________________________");
        System.out.println("Grade ID: " + theGrade.getGradeId());
        System.out.println("Student: " + student.getStudentId() + " - " + student.getName());
        System.out.println("Subject: " + selectedSubject.getSubjectName());
        System.out.println("Grade: " + gradeValue + "%");
        System.out.println("Date: " + theGrade.getDate());
        System.out.print("Confirm grade? (Y/N): ");

        String confirm = scanner.nextLine().trim();

        while (!confirm.equalsIgnoreCase("Y") && !confirm.equalsIgnoreCase("N")) {
            System.out.print("Invalid! Please enter Y or N: ");
            confirm = scanner.nextLine().trim();
        }

        if (confirm.equalsIgnoreCase("Y")) {
            System.out.println("Grade saved successfully!");

            double newAverage = calculateOverallAverage(id);
            int totalSubjects = getRegisteredSubjects(id);
            FileExporter.updateStudentGradeFile(student, theGrade, newAverage, totalSubjects);
            bulkImportService.saveOrUpdateGradeCSV(student, theGrade);

            // Process tasks from priority queue - O(k log n) where k is tasks processed
            processGradeTasks();

            System.out.println("Press enter to continue.");
            scanner.nextLine();
        } else {
            // Remove if cancelled
            gradeList.remove(theGrade); // O(n) worst case
            studentGradesMap.get(id).remove(theGrade); // O(n) worst case
            gradeHistory.removeFirst(); // O(1) from beginning
            System.out.println("Grade recording cancelled.");
            System.out.println("Press enter to continue.");
            scanner.nextLine();
        }
    }

    // Update GPA ranking in TreeMap - O(log n) for TreeMap operations
    private void updateStudentGpaRanking(Student student) {
        if (student == null) return;

        String studentId = student.getStudentId();
        double gpa = calculateOverallAverage(studentId);

        for (Map.Entry<Double, List<Student>> entry : gpaRankings.entrySet()) {
            entry.getValue().removeIf(s -> s.getStudentId().equals(studentId)); // O(n)
        }

        gpaRankings.entrySet().removeIf(entry -> entry.getValue().isEmpty()); // O(log n)

        gpaRankings.computeIfAbsent(gpa, k -> new ArrayList<>()).add(student);
    }

    // Display GPA rankings - O(n) iteration through TreeMap
    public void displayGpaRankings() {
        System.out.println("\n🏆 GPA RANKINGS (Descending Order)");
        System.out.println("=".repeat(60));
        int rank = 1;

        for (Map.Entry<Double, List<Student>> entry : gpaRankings.entrySet()) {
            double gpa = entry.getKey();
            for (Student student : entry.getValue()) {
                System.out.printf("%2d. %-12s %-25s GPA: %.2f (Type: %s)%n",
                        rank++, student.getStudentId(), student.getName(),
                        gpa, student.getStudentType());
            }
        }

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // Track courses for a student - O(n)
    private void trackStudentCourses(String studentId) {
        List<Grade> grades = studentGradesMap.get(studentId);
        if (grades != null) {
            for (Grade grade : grades) {
                uniqueCourses.add(grade.getSubject().getSubjectCode()); // O(1)
            }
        }
    }

    // Display unique courses - O(n) to iterate
    public void displayUniqueCourses() {
        System.out.println("\n📚 UNIQUE COURSES ENROLLED (" + uniqueCourses.size() + " courses)");
        System.out.println("=".repeat(40));

        // Convert to ArrayList for sorting
        List<String> sortedCourses = new ArrayList<>(uniqueCourses); // O(n)
        Collections.sort(sortedCourses); // O(n log n)

        for (String course : sortedCourses) {
            System.out.println("• " + course);
        }

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }


    // Add grade history entry - O(1) at beginning
    public void logGradeChange(String studentId, String subjectCode,
                               double oldGrade, double newGrade, String action) {
        gradeHistory.addFirst(new GradeHistoryEntry(
                studentId, subjectCode, oldGrade, newGrade, action
        ));

        // Keep history size manageable - O(1) remove from end
        if (gradeHistory.size() > 1000) {
            gradeHistory.removeLast();
        }
    }

    // Display recent grade history - O(k) where k is count
    public void displayRecentGradeHistory(int count) {
        System.out.println("\n📜 RECENT GRADE HISTORY (Last " + Math.min(count, gradeHistory.size()) + " entries)");
        System.out.println("=".repeat(80));

        Iterator<GradeHistoryEntry> it = gradeHistory.iterator(); // O(1)
        for (int i = 0; i < count && it.hasNext(); i++) {
            System.out.println(it.next());
        }

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // Get grades in insertion order - O(1) per element iteration
    public List<Grade> getGradesInsertionOrder() {
        return new ArrayList<>(gradeList); // O(n) copy
    }

    // Find grade by index (maintains insertion order) - O(1)
    public Grade getGradeByIndex(int index) {
        if (index >= 0 && index < gradeList.size()) {
            return gradeList.get(index); // O(1)
        }
        return null;
    }

        // Add task to queue - O(log n)
    public void scheduleGradeTask(String description, int priority, Runnable action) {
        taskQueue.add(new GradeTask(description, priority, action));
    }

    // Process tasks from queue - O(k log n) where k is tasks processed
    private void processGradeTasks() {
        System.out.println("\n Processing scheduled grade tasks...");
        int processed = 0;
        while (!taskQueue.isEmpty() && processed < 3) {
            GradeTask task = taskQueue.poll(); // O(log n)
            System.out.println("  ✓ " + task.getDescription());
            if (task.getTaskAction() != null) {
                task.getTaskAction().run();
            }
            processed++;
        }
        if (!taskQueue.isEmpty()) {
            System.out.println("  ⏳ " + taskQueue.size() + " tasks remaining in queue");
        }
    }

    // Display pending tasks - O(n) iteration
    public void displayPendingTasks() {
        System.out.println("\n📋 PENDING GRADE TASKS (" + taskQueue.size() + " tasks)");
        System.out.println("=".repeat(60));

        PriorityQueue<GradeTask> copy = new PriorityQueue<>(taskQueue);
        while (!copy.isEmpty()) {
            System.out.println(copy.poll());
        }

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private TreeSet<Double> uniqueGradeValues = new TreeSet<>(Collections.reverseOrder());

    // Update unique grades - O(log n)
    private void updateUniqueGrades(double grade) {
        uniqueGradeValues.add(grade);
    }

    // Display unique grades distribution - O(n) iteration
    public void displayGradeDistribution() {
        System.out.println("\n📊 UNIQUE GRADE DISTRIBUTION");
        System.out.println("=".repeat(40));

        int[] ranges = new int[10];
        for (Double grade : uniqueGradeValues) {
            int index = (int)(grade / 10);
            if (index >= 10) index = 9;
            ranges[index]++;
        }

        for (int i = 0; i < ranges.length; i++) {
            int start = i * 10;
            int end = (i == 9) ? 100 : start + 9;
            System.out.printf("%3d-%3d: %d students%n", start, end, ranges[i]);
        }
    }

    // Get students sorted by multiple criteria - O(n log n) sort
    public List<Student> getStudentsSortedByCriteria(Comparator<Student> comparator) {
        List<Student> allStudents = new ArrayList<>();

        // Get all students with grades - O(n)
        for (String studentId : studentGradesMap.keySet()) {
            Student student = studentManager.findStudent(studentId);
            if (student != null) {
                allStudents.add(student);
            }
        }

        // Sort using custom comparator - O(n log n)
        allStudents.sort(comparator);
        return allStudents;
    }

    // Display sorted students - O(n log n) for sort + O(n) for display
    public void displayStudentsSortedByMultipleCriteria() {
        System.out.println("\n📈 STUDENTS SORTED BY: Honors > GPA > Subjects > Name");
        System.out.println("=".repeat(80));

        List<Student> sorted = getStudentsSortedByCriteria(new StudentMultiComparator());

        System.out.printf("%-5s %-12s %-25s %-15s %-8s %-8s%n",
                "Rank", "ID", "Name", "Type", "GPA", "Subjects");
        System.out.println("-".repeat(80));

        for (int i = 0; i < sorted.size(); i++) {
            Student s = sorted.get(i);
            double gpa = calculateOverallAverage(s.getStudentId());
            int subjects = getRegisteredSubjects(s.getStudentId());

            System.out.printf("%-5d %-12s %-25s %-15s %-7.1f %-8d%n",
                    i + 1, s.getStudentId(), s.getName(),
                    s.getStudentType(), gpa, subjects);
        }

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }



    private Grade findExistingGrade(String studentId, String subjectCode) {
        // O(1) average lookup in HashMap, then O(n) search in list
        List<Grade> grades = studentGradesMap.get(studentId);
        if (grades != null) {
            for (Grade grade : grades) {
                if (grade.getSubject().getSubjectCode().equals(subjectCode)) {
                    return grade;
                }
            }
        }
        return null;
    }

    public double calculateOverallAverage(String studentId) {
        // O(1) average lookup, then O(n) calculation
        List<Grade> grades = studentGradesMap.get(studentId);
        if (grades == null || grades.isEmpty()) return 0;

        double total = 0;
        for (Grade grade : grades) {
            total += grade.getGrade();
        }
        return total / grades.size();
    }

    public int getRegisteredSubjects(String studentId) {
        // O(1) average lookup, then O(1) size check
        List<Grade> grades = studentGradesMap.get(studentId);
        return (grades != null) ? grades.size() : 0;
    }


    public void analyzeGradeStatistics() {
        System.out.println("\n📊 GRADE STATISTICS ANALYSIS");
        System.out.println("=".repeat(60));

        // Using TreeSet for sorted unique grades - O(1) for min/max
        if (!uniqueGradeValues.isEmpty()) {
            System.out.printf("Highest Grade: %.1f%n", uniqueGradeValues.first());
            System.out.printf("Lowest Grade: %.1f%n", uniqueGradeValues.last());
        }

        // Using HashSet for unique courses - O(1) size
        System.out.println("Unique Courses: " + uniqueCourses.size());

        // Using TreeMap for GPA distribution - O(log n) navigation
        System.out.println("GPA Distribution:");
        for (Map.Entry<Double, List<Student>> entry : gpaRankings.entrySet()) {
            System.out.printf("  GPA %.1f: %d students%n",
                    entry.getKey(), entry.getValue().size());
        }

        // Using LinkedList for history stats - O(1) size
        System.out.println("Grade History Entries: " + gradeHistory.size());

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }


    public void viewGradeByStudent(String studentId) {
        // O(1) average lookup in HashMap
        List<Grade> grades = studentGradesMap.get(studentId);
        if (grades == null || grades.isEmpty()) {
            System.out.println("No grades found for student: " + studentId);
            return;
        }

        // Display using ArrayList iteration - O(n)
        System.out.println("\nGrades for student: " + studentId);
        for (Grade grade : grades) {
            System.out.printf("  %s: %.1f%%%n",
                    grade.getSubject().getSubjectName(), grade.getGrade());
        }
    }

    // Initialize GPA rankings for all students - O(n log n)
    public void initializeGpaRankings() {
        gpaRankings.clear();
        Map<String, Student> allStudents = studentManager.getAllStudents();
        for (Student student : allStudents.values()) {
            updateStudentGpaRanking(student);
        }
    }

    // Get top performing students - O(log n) for TreeMap navigation
    public List<Student> getTopPerformers(int count) {
        List<Student> topPerformers = new ArrayList<>();
        for (Map.Entry<Double, List<Student>> entry : gpaRankings.entrySet()) {
            for (Student student : entry.getValue()) {
                if (topPerformers.size() >= count) break;
                topPerformers.add(student);
            }
            if (topPerformers.size() >= count) break;
        }
        return topPerformers;
    }

    // Clear all data structures - O(n) for each
    public void clearAllData() {
        gpaRankings.clear();
        uniqueCourses.clear();
        gradeHistory.clear();
        gradeList.clear();
        taskQueue.clear();
        studentGradesMap.clear();
        uniqueGradeValues.clear();
    }
}