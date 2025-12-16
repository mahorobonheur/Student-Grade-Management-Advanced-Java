package org.example.presentation;

import org.example.application.ServiceLocator;
import org.example.application.service.StudentService;
import org.example.application.service.GradeService;

import java.util.Scanner;

public class MenuController {
    private final ServiceLocator serviceLocator;
    private final Scanner scanner;

    public MenuController(ServiceLocator serviceLocator, Scanner scanner) {
        this.serviceLocator = serviceLocator;
        this.scanner = scanner;
    }

    public void startApplication() {
        displayWelcomeMessage();

        while (true) {
            displayMainMenu();
            int choice = getMenuChoice();

            if (choice == 19) {
                System.out.println("Thank you for using Student Grade Management System! Goodbye!");
                break;
            }

            processMenuChoice(choice);
        }
    }

    private void displayWelcomeMessage() {
        System.out.println("=================================================");
        System.out.println("|    STUDENT GRADE MANAGEMENT SYSTEM v2.0      |");
        System.out.println("=================================================");
    }

    private void displayMainMenu() {
        System.out.println("\nMAIN MENU");
        System.out.println("=".repeat(50));
        System.out.println("\nSTUDENT MANAGEMENT");
        System.out.println(" 1. Add Student");
        System.out.println(" 2. View All Students");
        System.out.println(" 3. View Student Details");

        System.out.println("\nGRADE MANAGEMENT");
        System.out.println(" 4. Record Grade");
        System.out.println(" 5. View Grade by Student");
        System.out.println(" 6. Export Grade Report (Multi-format)");

        System.out.println("\nANALYTICS & REPORTING");
        System.out.println(" 7. Calculate Student GPA");
        System.out.println(" 8. View Class Statistics");
        System.out.println(" 9. Search Students");

        System.out.println("\nDATA OPERATIONS");
        System.out.println("10. Bulk Import Grades");
        System.out.println("11. Export All Data");

        System.out.println("\nSYSTEM");
        System.out.println("19. Exit");
        System.out.println("=".repeat(50));
        System.out.print("\nEnter your choice (1-11, 19): ");
    }

    private int getMenuChoice() {
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if ((choice >= 1 && choice <= 11) || choice == 19) {
                    return choice;
                }
                System.out.print("Invalid choice! Enter 1-11 or 19: ");
            } catch (NumberFormatException e) {
                System.out.print("Please enter a number: ");
            }
        }
    }

    private void processMenuChoice(int choice) {
        StudentService studentService = serviceLocator.getStudentService();
        GradeService gradeService = serviceLocator.getGradeService();

        switch (choice) {
            case 1:
                studentService.addStudent(scanner);
                break;
            case 2:
                studentService.viewAllStudents();
                waitForEnter();
                break;
            case 3:
                viewStudentDetails(studentService);
                break;
            case 4:
                gradeService.addGrade(scanner);
                break;
            case 5:
                gradeService.viewGradeByStudent(scanner);
                waitForEnter();
                break;
            case 6:
                gradeService.exportGradeReport(scanner);
                waitForEnter();
                break;
            case 7:
                gradeService.calculateStudentGPA(scanner);
                waitForEnter();
                break;
            case 8:
                gradeService.viewClassStatistics();
                waitForEnter();
                break;
            case 9:
                gradeService.searchStudents(scanner);
                waitForEnter();
                break;
            case 10:
                gradeService.bulkImportGrades(scanner);
                waitForEnter();
                break;
            case 11:
                System.out.println("Export all data feature coming soon!");
                waitForEnter();
                break;
            default:
                System.out.println("Invalid option!");
                waitForEnter();
                break;
        }
    }

    private void viewStudentDetails(StudentService studentService) {
        System.out.print("\nEnter Student ID: ");
        String id = scanner.nextLine().trim();

        org.example.domain.model.Student student = studentService.findStudent(id);
        if (student != null) {
            student.displayStudentDetails();
        } else {
            System.out.println("Student not found!");
        }

        waitForEnter();
    }

    private void waitForEnter() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}