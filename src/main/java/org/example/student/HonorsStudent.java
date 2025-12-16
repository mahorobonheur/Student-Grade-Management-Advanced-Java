package org.example.student;

/**
 * This class manages student who is considered as Honors student
 */
public class HonorsStudent extends Student{
    private double passingGrade = 60;
    private boolean honorsEligible = false;
    
    public HonorsStudent(String name, int age, String email, String phone) {
        super(name, age, email, phone);
    }

    @Override
    public void displayStudentDetails() {
        System.out.println("   Student ID: " +getStudentId());
        System.out.println("   Name: " +getName());
        System.out.println("   Type: " +getStudentType());
        System.out.println("   Age: " +getAge());
        System.out.println("   Email: " +getEmail());
        System.out.println("   Passing Grade: " +getPassingGrade() + "%");

        if(checkHonorsEligibility()){
            System.out.println("   Honors Eligible: " + "Yes");
        } else{
            System.out.println("   Honors Eligible: " + "No");
        }
        System.out.println("   Status: " +getStatus());

    }

    @Override
    public String getStudentType() {
        return "Honors";
    }

    @Override
    public double getPassingGrade() {
        return passingGrade;
    }

    public boolean checkHonorsEligibility(){
        if (honorsEligible){
            return true;
        } else{
            return false;
        }
    }

    public boolean isHonorsEligible() {
        return honorsEligible;
    }

    public void setHonorsEligible(boolean honorsEligible) {
        this.honorsEligible = honorsEligible;
    }
}

