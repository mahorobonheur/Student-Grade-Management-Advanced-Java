package org.example.application.service;

import org.example.domain.model.*;
import org.example.domain.repository.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class SearchService {
    private final StudentRepository studentRepo;
    private final GradeRepository gradeRepo;

    public SearchService(StudentRepository studentRepo, GradeRepository gradeRepo) {
        this.studentRepo = studentRepo;
        this.gradeRepo = gradeRepo;
    }

    public void advancedSearch(Scanner scanner) {
        System.out.println("\nADVANCED SEARCH OPTIONS");
        System.out.println("=".repeat(40));
        System.out.println("1. Search by Email Domain");
        System.out.println("2. Search by Phone Area Code");
        System.out.println("3. Search by Student ID Pattern");
        System.out.println("4. Search by Name Pattern");
        System.out.println("5. Custom Regex Search");
        System.out.print("Select option: ");

        int option = Integer.parseInt(scanner.nextLine().trim());

        switch (option) {
            case 1:
                searchByEmailDomain(scanner);
                break;
            case 2:
                searchByAreaCode(scanner);
                break;
            case 3:
                searchByIDPattern(scanner);
                break;
            case 4:
                searchByNamePattern(scanner);
                break;
            case 5:
                customRegexSearch(scanner);
                break;
        }
    }

    public void patternSearch(Scanner scanner) {
        System.out.print("\nEnter regex pattern: ");
        String pattern = scanner.nextLine().trim();

        System.out.println("Searching with pattern: " + pattern);
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

        List<Student> matches = studentRepo.findAll().stream()
                .filter(student -> regex.matcher(student.getName()).find() ||
                        regex.matcher(student.getEmail()).find() ||
                        regex.matcher(student.getStudentId()).find())
                .collect(Collectors.toList());

        displaySearchResults(matches);
    }

    private void searchByEmailDomain(Scanner scanner) {
        System.out.print("Enter email domain (e.g., @university.edu): ");
        String domain = scanner.nextLine().trim();

        String pattern = ".*" + Pattern.quote(domain) + "$";
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

        List<Student> matches = studentRepo.findAll().stream()
                .filter(student -> regex.matcher(student.getEmail()).matches())
                .collect(Collectors.toList());

        System.out.println("\nEMAIL DOMAIN SEARCH RESULTS");
        System.out.println("=".repeat(60));
        displaySearchResults(matches);

        // Show domain distribution
        showDomainDistribution();
    }

    private void searchByAreaCode(Scanner scanner) {
        System.out.print("Enter area code (3 digits): ");
        String areaCode = scanner.nextLine().trim();

        String pattern = ".*" + areaCode + ".*";
        Pattern regex = Pattern.compile(pattern);

        List<Student> matches = studentRepo.findAll().stream()
                .filter(student -> regex.matcher(student.getPhone()).find())
                .collect(Collectors.toList());

        System.out.println("\nAREA CODE SEARCH RESULTS");
        System.out.println("=".repeat(60));
        displaySearchResults(matches);
    }

    private void searchByIDPattern(Scanner scanner) {
        System.out.print("Enter ID pattern (use * for wildcard): ");
        String pattern = scanner.nextLine().trim().replace("*", ".*");

        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

        List<Student> matches = studentRepo.findAll().stream()
                .filter(student -> regex.matcher(student.getStudentId()).matches())
                .collect(Collectors.toList());

        System.out.println("\nID PATTERN SEARCH RESULTS");
        System.out.println("=".repeat(60));
        displaySearchResults(matches);
    }

    private void searchByNamePattern(Scanner scanner) {
        System.out.print("Enter name pattern: ");
        String pattern = scanner.nextLine().trim();

        Pattern regex = Pattern.compile(".*" + Pattern.quote(pattern) + ".*",
                Pattern.CASE_INSENSITIVE);

        List<Student> matches = studentRepo.findAll().stream()
                .filter(student -> regex.matcher(student.getName()).find())
                .collect(Collectors.toList());

        System.out.println("\nNAME PATTERN SEARCH RESULTS");
        System.out.println("=".repeat(60));
        displaySearchResults(matches);
    }

    private void customRegexSearch(Scanner scanner) {
        System.out.print("Enter custom regex pattern: ");
        String pattern = scanner.nextLine().trim();

        try {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

            List<Student> matches = studentRepo.findAll().stream()
                    .filter(student ->
                            regex.matcher(student.getName()).find() ||
                                    regex.matcher(student.getEmail()).find() ||
                                    regex.matcher(student.getPhone()).find() ||
                                    regex.matcher(student.getStudentId()).find())
                    .collect(Collectors.toList());

            System.out.println("\nCUSTOM REGEX SEARCH RESULTS");
            System.out.println("=".repeat(60));
            displaySearchResults(matches);

            // Show match analysis
            analyzePatternMatches(matches, pattern);

        } catch (PatternSyntaxException e) {
            System.out.println("❌ Invalid regex pattern: " + e.getMessage());
        }
    }

    private void displaySearchResults(List<Student> students) {
        if (students.isEmpty()) {
            System.out.println("No matches found.");
            return;
        }

        System.out.printf("Found %d match(es):%n%n", students.size());
        System.out.printf("%-12s | %-25s | %-15s | %-30s%n",
                "Student ID", "Name", "Type", "Email");
        System.out.println("-".repeat(85));

        for (Student student : students) {
            System.out.printf("%-12s | %-25s | %-15s | %-30s%n",
                    student.getStudentId(),
                    student.getName(),
                    student.getStudentType(),
                    student.getEmail());
        }
    }

    private void showDomainDistribution() {
        Map<String, Long> domainCount = studentRepo.findAll().stream()
                .map(student -> {
                    String email = student.getEmail();
                    return email.substring(email.indexOf('@'));
                })
                .collect(Collectors.groupingBy(domain -> domain, Collectors.counting()));

        System.out.println("\nEMAIL DOMAIN DISTRIBUTION");
        System.out.println("-".repeat(40));
        domainCount.forEach((domain, count) ->
                System.out.printf("%-25s: %d students%n", domain, count));
    }

    private void analyzePatternMatches(List<Student> matches, String pattern) {
        System.out.println("\nPATTERN MATCH ANALYSIS");
        System.out.println("-".repeat(40));
        System.out.println("Total students scanned: " + studentRepo.findAll().size());
        System.out.println("Matches found: " + matches.size());
        System.out.printf("Match rate: %.1f%%%n",
                (matches.size() * 100.0 / studentRepo.findAll().size()));
        System.out.println("Pattern complexity: " +
                (pattern.length() > 20 ? "HIGH" : pattern.length() > 10 ? "MEDIUM" : "LOW"));
    }
}