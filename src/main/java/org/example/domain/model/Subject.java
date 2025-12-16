package org.example.domain.model;

public abstract class Subject {
    protected String subjectName;
    protected String subjectCode;

    public Subject(String subjectName, String subjectCode) {
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
    }

    public abstract String getSubjectType();

    public String getSubjectName() { return subjectName; }
    public String getSubjectCode() { return subjectCode; }
}