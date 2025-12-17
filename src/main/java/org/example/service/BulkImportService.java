package org.example.service;

import org.example.grade.Grade;
import org.example.student.Student;
import org.example.subject.Subject;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class BulkImportService {

    private static final String IMPORT_DIR = "./reports/";

    static {
        File dir = new File(IMPORT_DIR);
        if (!dir.exists()) dir.mkdirs();
    }


    public void saveOrUpdateGradeCSV(Student student, Grade grade) {
        String filename = IMPORT_DIR + student.getStudentId() + "_grades.csv";
        File file = new File(filename);

        List<String> lines = new ArrayList<>();
        boolean updated = false;

        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4 && parts[1].equalsIgnoreCase(grade.getSubject().getSubjectName())) {
                        lines.add(student.getStudentId() + "," +
                                grade.getSubject().getSubjectName() + "," +
                                grade.getSubject().getSubjectType() + "," +
                                grade.getGrade());
                        updated = true;
                    } else {
                        lines.add(line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading existing CSV: " + e.getMessage());
            }
        }


        if (!updated) {
            lines.add(student.getStudentId() + "," +
                    grade.getSubject().getSubjectName() + "," +
                    grade.getSubject().getSubjectType() + "," +
                    grade.getGrade());
        }

        try {
            Files.write(Paths.get(filename), lines);
        } catch (IOException e) {
            System.out.println("Error writing CSV: " + e.getMessage());
        }
    }

    /**
     * Reads grades from CSV for bulk import
     */
    public List<String[]> readGradesFromCSV(String filename) throws IOException {
        List<String[]> grades = new ArrayList<>();
        File file = new File(IMPORT_DIR + filename);
        if (!file.exists()) throw new FileNotFoundException("File not found: " + filename);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    grades.add(line.split(","));
                }
            }
        }
        return grades;
    }
}
