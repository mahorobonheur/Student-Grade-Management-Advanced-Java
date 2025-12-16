package org.example.domain.model;

import java.io.Serializable;

public abstract class Student implements Serializable {
    protected String studentId;
    protected String name;
    protected int age;
    protected String email;
    protected String phone;
    protected String enrollmentDate;
    protected String status;
    protected String studentType;

    public Student(String name, int age, String email, String phone) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.phone = phone;
    }

    public abstract double getPassingGrade();

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(String enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStudentType() { return studentType; }
    public void setStudentType(String studentType) { this.studentType = studentType; }

    public void displayStudentDetails() {
        System.out.println("\nSTUDENT DETAILS");
        System.out.println("========================");
        System.out.println("ID: " + studentId);
        System.out.println("Name: " + name);
        System.out.println("Age: " + age);
        System.out.println("Email: " + email);
        System.out.println("Phone: " + phone);
        System.out.println("Type: " + studentType);
        System.out.println("Enrollment Date: " + enrollmentDate);
        System.out.println("Passing Grade: " + getPassingGrade() + "%");
        System.out.println("Status: " + status);
    }
}