package org.example.subject;

public class CoreSubject extends Subject{
    private boolean mandatory = true;

    public CoreSubject(String subjectName, String subjectCode) {
        super(subjectName, subjectCode);
    }



    @Override
    void displaySubjectDetails() {

    }

    @Override
    public String getSubjectType() {
        return "Core";
    }

    public boolean isMandatory(){
        return mandatory;
    }
}

