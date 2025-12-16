package org.example.student;


import org.example.grade.GradeManager;

/**
 * The abstract class that holds methods that will be used by subclasses which will override its methods and implement them in different ways.
 */

public abstract class Student {
    private String studentId;
    private String name;
    private String enrollmentDate;
    private int age;
    private String email;
    private String phone;
    private String status = "Active";
    static int studentCounter = 1;

    protected GradeManager gradeManager;
    protected StudentManager studentManager;


    public Student(){

    }
    public Student(String name, int age, String email, String phone) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.phone = phone;
    }

    public abstract void displayStudentDetails();
    public abstract String getStudentType();
    public abstract double getPassingGrade();
    public double calculateAverageGrade(){
        double avg = gradeManager.calculateOverallAverage(this.studentId);
        return avg;
    }



    public String isPassing(String id){
        double passingRequirement = (studentManager.findStudent(id) instanceof RegularStudent) ? 50 : 60;
        if(gradeManager.calculateOverallAverage(id) < passingRequirement){
            return  "Failing";
        } else {
            return "Passing";
        }
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(String enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public static int getStudentCounter() {
        return studentCounter;
    }

    public static void setStudentCounter(int studentCounter) {
        Student.studentCounter = studentCounter;
    }

    public GradeManager getGradeManager() {
        return gradeManager;
    }

    public void setGradeManager(GradeManager gradeManager) {
        this.gradeManager = gradeManager;
    }

    public StudentManager getStudentManager() {
        return studentManager;
    }


    public void setStudentManager(StudentManager studentManager) {
        this.studentManager = studentManager;
    }

    public double calculateGPA() {
        return 0.0;
    }

    public String getLetterGrade() {
        double average = calculateAverageGrade();
        if (average >= 93) return "A";
        else if (average >= 90) return "A-";
        else if (average >= 87) return "B+";
        else if (average >= 83) return "B";
        else if (average >= 80) return "B-";
        else if (average >= 77) return "C+";
        else if (average >= 73) return "C";
        else if (average >= 70) return "C-";
        else if (average >= 67) return "D+";
        else if (average >= 60) return "D";
        else return "F";
    }


}
