package com.project;

import com.project.service.CitizenService;
import com.project.service.ExcelImportService;
import com.project.util.HibernateUtil;
import org.hibernate.SessionFactory;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n========================================");
            System.out.println("   RELATIVE IDENTIFICATION SYSTEM       ");
            System.out.println("========================================");
            System.out.println("1. Register Citizen / Family Member (Manual)");
            System.out.println("2. Link Two Families (Inter-Family)");
            System.out.println("3. View Household Members");
            System.out.println("4. Import Data from Excel (Data-Driven)");
            System.out.println("5. Exit");
            System.out.print("Enter your choice (1-5): ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    CitizenService.registerCitizen(sf, scanner);
                    break;
                case 2:
                    CitizenService.linkFamilies(sf, scanner);
                    break;
                case 3:
                    CitizenService.viewHousehold(sf, scanner);
                    break;
                case 4:
                    ExcelImportService.importDataFromExcel(sf);
                    break;
                case 5:
                    running = false;
                    System.out.println("Shutting down system. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 5.");
            }
        }

        scanner.close();
        HibernateUtil.shutdown();
    }
}