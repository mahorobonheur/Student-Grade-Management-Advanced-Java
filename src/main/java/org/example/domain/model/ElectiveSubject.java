package org.example.domain.model;

public class ElectiveSubject extends Subject {
    public ElectiveSubject(String subjectName, String subjectCode) {
        super(subjectName, subjectCode);
    }

    @Override
    public String getSubjectType() {
        return "Elective";
    }
}