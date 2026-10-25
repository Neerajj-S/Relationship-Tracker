package org.relativesdb.connection;

import org.relativesdb.connection.service.CitizenService;
import org.relativesdb.connection.service.ExcelImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
public class RelativesConnectionApplication implements CommandLineRunner {

    @Autowired
    private CitizenService citizenService;

    @Autowired
    private ExcelImportService excelImportService;

    public static void main(String[] args) {
        SpringApplication.run(RelativesConnectionApplication.class, args);
    }

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
        	System.out.println("\n========================================");
        	System.out.println("       RELATIVE IDENTIFICATION SYSTEM    ");
        	System.out.println("========================================");
        	System.out.println("1. Link Two Families");
        	System.out.println("2. Search Citizen by Name");
        	System.out.println("3. Import Data from Excel");
        	System.out.println("4. Promote Existing Child to New Head");
        	System.out.println("5. Transfer Family Members to New Head");
        	System.out.println("6. Exit");
        	System.out.print("Enter your choice (1-6): ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    citizenService.linkFamilies(scanner);
                    break;
                case 2:
                    citizenService.searchCitizenByName(scanner);
                    break;
                case 3:
                    excelImportService.importDataFromExcel();
                    break;
                case 4:
                    citizenService.promoteExistingChildToHead(scanner);
                    break;
                case 5:
                    citizenService.transferFamilyMembers(scanner);
                    break;
                case 6:
                    System.out.println("Exiting application. Goodbye!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        scanner.close();
    }
}