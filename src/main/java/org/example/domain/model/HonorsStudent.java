package org.example.domain.model;

public class HonorsStudent extends Student {
    private static final double PASSING_GRADE = 60.0;

    public HonorsStudent(String name, int age, String email, String phone) {
        super(name, age, email, phone);
        this.studentType = "Honors";
    }

    @Override
    public double getPassingGrade() {
        return PASSING_GRADE;
    }
}