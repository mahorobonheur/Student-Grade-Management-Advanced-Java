package org.example.student;

import org.example.grade.GradeManager;
import org.example.newImprementations.FileExporter;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class StudentManager {
    private Map<String, Student> studentMap = new HashMap<>();
    private int studentCount;

    Scanner scanner = new Scanner(System.in);
    GradeManager gradeManager = new GradeManager();

    private String generateStudentId() {
        studentCount++;
        return String.format("STU%03d", studentCount);
    }


    public void addStudent(Student students){
        System.out.println("ADD STUDENT");
        System.out.println("_________________________");
        System.out.println("Enter student name: ");
        String studentName = scanner.nextLine();

        while (true) {
            if (studentName.length() < 3) {
                System.out.print("Please enter a name with more than 3 characters: ");
            } else if (!studentName.matches("^[a-zA-Z]+([-'\\s][a-zA-Z]+)*$")) {
                System.out.println("Pattern required: Only letters, spaces, and apostrophes");
                System.out.println(" Eg: John Smith, Marry-Jane, O'Connor");
            } else {
                break;
            }
            studentName = scanner.nextLine().trim();
        }

        System.out.print("Enter age: ");
        int age = 0;

        while (true) {
            String input = scanner.nextLine().trim();
            try {
                age = Integer.parseInt(input);

                if (age >= 5 && age <= 120) break;

                System.out.print("Enter a valid age (5–120): ");
            }
            catch (NumberFormatException e) {
                System.out.print("Age must be a number: ");
            }
        }

        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();

        while (true) {
            if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                System.out.print("Invalid email format. Enter again: ");
            } else {
                break;
            }
            email = scanner.nextLine().trim();
        }


        System.out.print("Enter phone: ");
        String phone = scanner.nextLine().trim();

        while (true) {
            String p = phone.startsWith("+") ? phone.substring(1) : phone;
            if (!p.matches( "^(\\(\\d{3}\\) \\d{3}-\\d{4}|\\d{3}-\\d{3}-\\d{4}|\\+1-\\d{3}-\\d{3}-\\d{4}|\\d{10})$")) {
                System.out.print("Invalid phone number (10–13 digits). Enter again: ");
            } else {
                break;
            }
            phone = scanner.nextLine().trim();
        }


        System.out.println("Student Type: ");
        System.out.println("1. Regular Student (Passing grade: 50%)");
        System.out.println("2. Honors Student (Passing grade: 60%, honors recognition)");
        int studentTypeChoice = 0;

        System.out.print("Enter Enrollment Date (YYYY-MM-DD): ");
        String enrollmentDate = scanner.nextLine().trim();

        while (!enrollmentDate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            System.out.print("Invalid date format. Please enter YYYY-MM-DD: ");
            enrollmentDate = scanner.nextLine().trim();
        }


        while (true) {
            try {
                studentTypeChoice = Integer.parseInt(scanner.nextLine().trim());

                if (studentTypeChoice == 1 || studentTypeChoice == 2) {
                    break;
                }

                System.out.print("Invalid choice! Enter 1 or 2: ");
            }
            catch (NumberFormatException e) {
                System.out.print("Invalid input! Enter a NUMBER (1 or 2): ");
            }
        }

        Student newStudent;
        String studentId = generateStudentId();
        if(studentTypeChoice == 1){
            newStudent = new RegularStudent(studentName, age, email, phone);

            newStudent.setStudentId(studentId);
            newStudent.setEnrollmentDate(enrollmentDate);
            newStudent.setGradeManager(this.gradeManager);
            newStudent.setStudentManager(this);

        } else {
            newStudent = new HonorsStudent(studentName, age, email, phone);

            newStudent.setStudentId(studentId);
            newStudent.setEnrollmentDate(enrollmentDate);
            newStudent.setStudentManager(this);
            newStudent.setGradeManager(this.gradeManager);

        }

        studentMap.put(studentId, newStudent);
        System.out.println("✅ Student added successfully!");
        newStudent.displayStudentDetails();

        System.out.println("\n📁 Auto-exporting student data to file...");
        double averageGrade = gradeManager.calculateOverallAverage(newStudent.getStudentId());
        int subjectCount = gradeManager.getRegisteredSubjects(newStudent.getStudentId());
        FileExporter.exportStudentToFile(newStudent, averageGrade, subjectCount);

        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    public Student findStudent(String studentId){
        Student s = studentMap.get(studentId);
        if (s == null) System.out.println("Student with ID: " + studentId + " not found.");
        return s;
    }

    public void viewAllStudents(){
        long regularCount = studentMap.values().stream().filter(s -> s.getStudentType().equals("Regular")).count();
        long honorsCount = studentMap.values().stream().filter(s -> s.getStudentType().equals("Honors")).count();

        if (regularCount < 3 || honorsCount < 2) {
            System.out.println("Cannot display listing.");
            System.out.println("Minimum requirements:");
            System.out.println("- At least 3 Regular Students (current: " + regularCount + ")");
            System.out.println("- At least 2 Honors Students (current: " + honorsCount + ")");
            scanner.nextLine();
            return;
        }

        System.out.println("STUDENT LISTING");
        System.out.println("___________________________________________________________________________________________________");
        System.out.printf("%-15s | %-25s | %-20s | %-12s | %-10s %n", "STU ID", " NAME", "TYPE", "AVG GRADE", "STATUS");
        System.out.println("---------------------------------------------------------------------------------------------------");

        int displayCount = 0;

        for(Student stud : studentMap.values()){

            if(stud != null ){
                double averageGrades = gradeManager.calculateOverallAverage(stud.getStudentId());

                System.out.printf("%-15s | %-25s | %-20s | %-12s | %-10s%n",
                        stud.getStudentId(),
                        stud.getName(),
                        stud.getStudentType(),
                        String.format("%.1f%%", averageGrades),
                        stud.isPassing(stud.getStudentId()));

                int totalSubjects = gradeManager.getRegisteredSubjects(stud.getStudentId());
                if(stud.getStudentType().equals("Regular")) {
                    System.out.printf("%-15s | %-28s | %-35s %n", " ", "Enrolled Subjects: " +totalSubjects, "Passing Grade: " + stud.getPassingGrade() +"%");
                    System.out.println("---------------------------------------------------------------------------------------------------");
                } else{
                    if(gradeManager.calculateOverallAverage(stud.getStudentId()) >= 85){
                        System.out.printf("%-15s | %-28s | %-28s  | %-25s %n", " ", "Enrolled Subjects: "  +totalSubjects, "Passing Grade: " + stud.getPassingGrade() + "%", "Honors Eligible");
                        System.out.println("---------------------------------------------------------------------------------------------------");
                    }
                    else {
                        System.out.printf("%-15s | %-28s | %-28s  | %-25s %n", " ", "Enrolled Subjects: "  +totalSubjects, "Passing Grade: " + stud.getPassingGrade() +"%", "Not Honors Eligible");
                        System.out.println("---------------------------------------------------------------------------------------------------");
                    }
                }
                displayCount++;
            }
        }

        System.out.println("\nTotal Students: " + displayCount);
        System.out.printf("Average Class Grade: %.1f %% ", getAverageClassGrade());
        System.out.println();
        System.out.println("\nPress enter to continue");
        scanner.nextLine();
    }

    public double getAverageClassGrade() {
        double total = 0;
        double countedStudents = 0;

        for (Student s : studentMap.values()) {
            if (s != null) {
                double avg = s.calculateAverageGrade();
                if (avg > 0) {
                    total += avg;
                    countedStudents++;
                }
            }
        }

        if (countedStudents == 0) return 0;
        return total / countedStudents;
    }

    public void exportStudentReport() {
        System.out.println("EXPORT STUDENT REPORT");
        System.out.println("__________________________");
        System.out.print("Enter Student ID: ");
        String id = scanner.nextLine().trim();

        Student student = findStudent(id);
        if (student == null) {
            System.out.println("Student with ID " + id + " not found!");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        double averageGrade = gradeManager.calculateOverallAverage(id);
        int subjectCount = gradeManager.getRegisteredSubjects(id);

        FileExporter.exportStudentToFile(student, averageGrade, subjectCount);

        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    public void exportAllStudentsReport() {
        if (studentCount == 0) {
            System.out.println("No students available to export!");
            System.out.println("Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        FileExporter.exportAllStudentsToFile(
                studentMap.values().toArray(new Student[0]), studentMap.size(), getAverageClassGrade());

        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    public int getStudentCount() {

        return studentMap.size();
    }

    public Map<String, Student> getAllStudents() {
        return studentMap;
    }

    public void setGradeManager(GradeManager gradeManager) {
        this.gradeManager = gradeManager;
    }
}