package org.example.domain.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Grade implements Serializable {
    private String gradeId;
    private String studentId;
    private Subject subject;
    private double grade;
    private String date;

    public Grade(String studentId, Subject subject, double grade) {
        this.studentId = studentId;
        this.subject = subject;
        this.grade = grade;
        this.date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void displayGradeDetails() {
        System.out.printf("%-15s | %-20s | %-12s | %-10s%n",
                gradeId, date, subject.getSubjectCode(), grade + "%");
    }

    public String getGradeId() { return gradeId; }
    public void setGradeId(String gradeId) { this.gradeId = gradeId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public double getGrade() { return grade; }
    public void setGrade(double grade) { this.grade = grade; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}