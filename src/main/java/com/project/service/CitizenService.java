package com.project.service;

import com.project.model.Citizen;
import com.project.model.Relations;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class CitizenService {

    public static void registerCitizen(SessionFactory sf, Scanner scanner) {
        try (Session session = sf.openSession()) {
            Transaction tx = session.beginTransaction();
            Citizen citizen = new Citizen();

            System.out.print("Is this person a Family Head? (yes/no): ");
            String isHead = scanner.nextLine().trim().toLowerCase();

            if (isHead.equals("yes")) {
                citizen.setRelationToHead("SELF");
                citizen.setFamilyHead(null);
                System.out.println("-> Registering as a Family Head.");
            } else {
                System.out.print("Enter the National ID of their Family Head: ");
                String headNid = scanner.nextLine();

                Citizen head = session.createQuery("FROM Citizen WHERE nationalId = :nid", Citizen.class)
                                      .setParameter("nid", headNid)
                                      .uniqueResult();

                if (head != null) {
                    citizen.setFamilyHead(head);
                    System.out.print("Enter relation to Head (e.g., SPOUSE, CHILD): ");
                    citizen.setRelationToHead(scanner.nextLine().toUpperCase());
                } else {
                    System.out.println("Error: Family Head not found in database!");
                    tx.rollback();
                    return;
                }
            }

            System.out.print("Enter THEIR OWN unique National ID: ");
            citizen.setNationalId(scanner.nextLine());

            System.out.print("Enter Full Name: ");
            citizen.setFullName(scanner.nextLine());

            System.out.print("Enter Gender (Male/Female/Other): ");
            citizen.setGender(scanner.nextLine());

            System.out.print("Enter Date of Birth (YYYY-MM-DD): ");
            citizen.setDateOfBirth(LocalDate.parse(scanner.nextLine()));

            session.persist(citizen);
            tx.commit();
            System.out.println("Citizen successfully registered!");

        } catch (Exception e) {
            System.out.println("Error registering citizen: " + e.getMessage());
        }
    }

    public static void linkFamilies(SessionFactory sf, Scanner scanner) {
        try (Session session = sf.openSession()) {
            Transaction tx = session.beginTransaction();

            System.out.print("Enter Family A Head National ID: ");
            String headAId = scanner.nextLine();

            System.out.print("Enter Family B Head National ID: ");
            String headBId = scanner.nextLine();

            Citizen headA = session.createQuery("FROM Citizen WHERE nationalId = :nid", Citizen.class)
                                  .setParameter("nid", headAId).uniqueResult();

            Citizen headB = session.createQuery("FROM Citizen WHERE nationalId = :nid", Citizen.class)
                                  .setParameter("nid", headBId).uniqueResult();

            if (headA != null && headB != null) {
                Relations relation = new Relations();
                relation.setFamilyAHead(headA);
                relation.setFamilyBHead(headB);

                System.out.print("Enter relationship type between families (e.g., IN_LAWS): ");
                relation.setRelationType(scanner.nextLine().toUpperCase());

                session.persist(relation);
                tx.commit();
                System.out.println("Families successfully linked!");
            } else {
                System.out.println("One or both Family Heads could not be found.");
            }
        } catch (Exception e) {
            System.out.println("Error linking families: " + e.getMessage());
        }
    }

    public static void viewHousehold(SessionFactory sf, Scanner scanner) {
        try (Session session = sf.openSession()) {
            System.out.print("Enter Family Head National ID to view household: ");
            String headNid = scanner.nextLine();

            Citizen head = session.createQuery("FROM Citizen WHERE nationalId = :nid", Citizen.class)
                                  .setParameter("nid", headNid).uniqueResult();

            if (head == null) {
                System.out.println("No Family Head found with that National ID.");
                return;
            }

            List<Citizen> members = session.createQuery(
                "FROM Citizen WHERE familyHead.nationalId = :nid", Citizen.class)
                .setParameter("nid", headNid).getResultList();

            System.out.println("\n----------------------------------------");
            System.out.println(" HOUSEHOLD RECORD FOR: " + head.getFullName());
            System.out.println("----------------------------------------");
            System.out.println(" -> Head: " + head.getFullName() + " | NID: " + head.getNationalId());
            
            System.out.println("\nRegistered Family Members (" + members.size() + "):");
            for (int i = 0; i < members.size(); i++) {
                Citizen m = members.get(i);
                System.out.println(" " + (i + 1) + ". " + m.getFullName() + 
                                   " [Relation: " + m.getRelationToHead() + " | NID: " + m.getNationalId() + "]");
            }
            System.out.println("----------------------------------------");
        } catch (Exception e) {
            System.out.println("Error fetching household data: " + e.getMessage());
        }
    }
}