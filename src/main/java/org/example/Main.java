package org.example;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice = 0;

        do{

            System.out.println("================================================");
            System.out.println("|      STUDENT GRADE MANAGEMENT - MAIN MENU     |");
            System.out.println("=================================================");
            System.out.println("\nSTUDENT MANAGEMENT\n");
            System.out.println(" 1. Add Student (with validation)");
            System.out.println(" 2. View Students");
            System.out.println(" 3. Record Grade");
            System.out.println(" 4. View Grade Report");
            System.out.println("\nFILE OPERATIONS\n");
            System.out.println(" 5. Export Grade Report (CSV/JSON/Binary \n");
            System.out.println(" 6. Import Data (Multi-format support)\n");
            System.out.println(" 7. Bulk Import Grades\n");
            System.out.println("\nANALYTICS & REPORTING\n");
            System.out.println(" 8. Calculate Student GPA\n");
            System.out.println(" 9. View Class Statistics\n");
            System.out.println(" 10. Real-Time Statistics Dashboard\n");
            System.out.println(" 11. Generate Batch Reports");
            System.out.println("\nSEARCH AND QUERY\n");
            System.out.println(" 12. Search Students Advanced\n");
            System.out.println(" 13. Pattern Based Search\n");
            System.out.println(" 14. Query Grade History\n");
            System.out.println("\nADVANCED FEATURES");
            System.out.println(" 15. Schedule Automated Tasks\n");
            System.out.println(" 16. View System Performance\n");
            System.out.println(" 17. Cache Management\n");
            System.out.println(" 18. Audit Trail Viewer\n");
            System.out.println("\n 19. Exit\n");
            System.out.println("Enter choice: ");

            try {
                choice = scanner.nextInt();
                scanner.nextLine();

                while (choice < 0 || choice > 19) {
                    System.out.println("Please enter a valid choice (1-19) : ");
                    choice = scanner.nextInt();
                }

            } catch (InputMismatchException exception){

                System.out.println("Please enter a real number");
                scanner.nextLine();
                continue;
            }


            switch (choice) {
                case 1:


                    break;
                case 2:

                    break;
                case 3:

                    break;
                case 4:

                    break;
                case 5:

                    break;

                case 19:
                    System.out.println("Thank you for using Student Grade Management System! \nGoodbye!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice");
                    break;
            }


        }while(true);

    }
}