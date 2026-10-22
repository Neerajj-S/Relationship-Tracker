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
            System.out.println("2. Link Two Families (Inter-Family Alliance)");
            System.out.println("3. View Household Members & Kinship Map");
            System.out.println("4. View Family Alliances");
            System.out.println("5. Search Citizen by Name");
            System.out.println("6. Analyze Kinship Between Two People");
            System.out.println("7. Import Data from Excel (Data-Driven)");
            System.out.println("8. Exit");

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
                CitizenService.viewFamilyAlliances(sf, scanner);
                break;
            case 5:
                CitizenService.searchCitizenByName(sf, scanner);
                break;
            case 6:
                CitizenService.analyzeKinship(sf, scanner);
                break;
            case 7:
                ExcelImportService.importDataFromExcel(sf);
                break;
            case 8:
                running = false;
                System.out.println("Shutting down system. Goodbye!");
                break;
            default:
                System.out.println("Invalid choice. Please enter a number between 1 and 7.");
            }
        }

        scanner.close();
        HibernateUtil.shutdown();
    }
}