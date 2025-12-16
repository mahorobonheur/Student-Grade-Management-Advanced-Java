package org.example.subject;

public class ElectiveSubject extends Subject{

    private boolean mandatory = false;

    public ElectiveSubject(String subjectName, String subjectCode) {
        super(subjectName, subjectCode);
    }

    @Override
    void displaySubjectDetails() {

    }

    @Override
    public String getSubjectType() {
        return "Elective";
    }

    public boolean isMandatory(){
        return mandatory;
    }

}

