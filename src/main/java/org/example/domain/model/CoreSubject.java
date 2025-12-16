package org.example.domain.model;

public class CoreSubject extends Subject {
    public CoreSubject(String subjectName, String subjectCode) {
        super(subjectName, subjectCode);
    }

    @Override
    public String getSubjectType() {
        return "Core";
    }
}