package org.example.domain.model;

public class RegularStudent extends Student {
    private static final double PASSING_GRADE = 50.0;

    public RegularStudent(String name, int age, String email, String phone) {
        super(name, age, email, phone);
        this.studentType = "Regular";
    }

    @Override
    public double getPassingGrade() {
        return PASSING_GRADE;
    }
}