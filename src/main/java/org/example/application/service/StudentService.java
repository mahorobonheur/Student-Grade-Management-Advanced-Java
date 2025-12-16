package org.example.application.service;

import org.example.domain.model.*;
import org.example.domain.repository.*;
import org.example.infrastructure.util.*;

import java.util.*;

public class StudentService {
    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final ValidationService validationService;
    private final ExportService exportService;
    private int studentCount = 0;

    public StudentService(StudentRepository studentRepository,
                          GradeRepository gradeRepository,
                          ValidationService validationService,
                          ExportService exportService) {
        this.studentRepository = studentRepository;
        this.gradeRepository = gradeRepository;
        this.validationService = validationService;
        this.exportService = exportService;
    }

    public void addStudent(Scanner scanner) {
        System.out.println("\nADD STUDENT");
        System.out.println("=".repeat(30));

        String studentName = promptForName(scanner);
        int age = promptForAge(scanner);
        String email = promptForEmail(scanner);
        String phone = promptForPhone(scanner);
        String enrollmentDate = promptForEnrollmentDate(scanner);
        Student student = promptForStudentType(scanner, studentName, age, email, phone);

        String studentId = generateStudentId();
        student.setStudentId(studentId);
        student.setEnrollmentDate(enrollmentDate);
        student.setStatus("ACTIVE");

        studentRepository.save(student);

        System.out.println("✅ Student added successfully!");
        student.displayStudentDetails();

        exportService.exportStudentToFile(student, 0.0, 0);

        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private String promptForName(Scanner scanner) {
        System.out.print("Enter student name: ");
        String name = scanner.nextLine().trim();

        while (true) {
            ValidationService.ValidationResult result = validationService.validateName(name);
            if (result.isValid()) break;
            System.out.print(result.getErrorMessage());
            name = scanner.nextLine().trim();
        }
        return name;
    }

    private int promptForAge(Scanner scanner) {
        System.out.print("Enter age (5-120): ");
        while (true) {
            try {
                int age = Integer.parseInt(scanner.nextLine().trim());
                if (age >= 5 && age <= 120) return age;
                System.out.print("Enter a valid age (5–120): ");
            } catch (NumberFormatException e) {
                System.out.print("Age must be a number: ");
            }
        }
    }

    private String promptForEmail(Scanner scanner) {
        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();

        while (true) {
            ValidationService.ValidationResult result = validationService.validateEmail(email);
            if (result.isValid()) break;
            System.out.print(result.getErrorMessage());
            email = scanner.nextLine().trim();
        }
        return email;
    }

    private String promptForPhone(Scanner scanner) {
        System.out.print("Enter phone: ");
        String phone = scanner.nextLine().trim();

        while (true) {
            ValidationService.ValidationResult result = validationService.validatePhone(phone);
            if (result.isValid()) break;
            System.out.print(result.getErrorMessage());
            phone = scanner.nextLine().trim();
        }
        return phone;
    }

    private String promptForEnrollmentDate(Scanner scanner) {
        System.out.print("Enter Enrollment Date (YYYY-MM-DD): ");
        String date = scanner.nextLine().trim();

        while (true) {
            ValidationService.ValidationResult result = validationService.validateDate(date);
            if (result.isValid()) break;
            System.out.print(result.getErrorMessage());
            date = scanner.nextLine().trim();
        }
        return date;
    }

    private Student promptForStudentType(Scanner scanner, String name, int age, String email, String phone) {
        System.out.println("\nStudent Type:");
        System.out.println("1. Regular Student (Passing grade: 50%)");
        System.out.println("2. Honors Student (Passing grade: 60%, honors recognition)");
        System.out.print("Select (1-2): ");

        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice == 1) return new RegularStudent(name, age, email, phone);
                if (choice == 2) return new HonorsStudent(name, age, email, phone);
                System.out.print("Invalid choice! Enter 1 or 2: ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid input! Enter a NUMBER (1 or 2): ");
            }
        }
    }

    private String generateStudentId() {
        studentCount++;
        return String.format("STU%03d", studentCount);
    }

    public void viewAllStudents() {
        List<Student> students = studentRepository.findAll();

        long regularCount = students.stream()
                .filter(s -> s.getStudentType().equals("Regular"))
                .count();
        long honorsCount = students.stream()
                .filter(s -> s.getStudentType().equals("Honors"))
                .count();

        if (regularCount < 3 || honorsCount < 2) {
            System.out.println("Cannot display listing. Minimum requirements:");
            System.out.println("- At least 3 Regular Students (current: " + regularCount + ")");
            System.out.println("- At least 2 Honors Students (current: " + honorsCount + ")");
            return;
        }

        System.out.println("\nSTUDENT LISTING");
        System.out.println("=".repeat(100));
        System.out.printf("%-15s | %-25s | %-15s | %-12s | %-10s%n",
                "STU ID", "NAME", "TYPE", "AVG GRADE", "STATUS");
        System.out.println("-".repeat(100));

        for (Student student : students) {
            double average = gradeRepository.calculateStudentAverage(student.getStudentId());
            String status = average >= student.getPassingGrade() ? "Passing" : "Failing";

            System.out.printf("%-15s | %-25s | %-15s | %-11.1f%% | %-10s%n",
                    student.getStudentId(),
                    student.getName(),
                    student.getStudentType(),
                    average,
                    status);
        }

        System.out.println("\nTotal Students: " + students.size());
        System.out.printf("Average Class Grade: %.1f%%\n", calculateClassAverage());
    }

    private double calculateClassAverage() {
        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) return 0.0;

        double total = students.stream()
                .mapToDouble(s -> gradeRepository.calculateStudentAverage(s.getStudentId()))
                .sum();
        return total / students.size();
    }

    public Student findStudent(String studentId) {
        return studentRepository.findById(studentId);
    }

    public List<Student> findAllStudents() {
        return studentRepository.findAll();
    }
}