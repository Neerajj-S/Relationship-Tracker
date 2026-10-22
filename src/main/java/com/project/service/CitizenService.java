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
            
            if (citizenExists(session, citizen.getNationalId())) {
                System.out.println("Error: A citizen with National ID " + citizen.getNationalId() + " already exists!");
                tx.rollback();
                return;
            }

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

            System.out.println("\n========================================");
            System.out.println(" HOUSEHOLD RECORD & KINSHIP MAPPING");
            System.out.println("========================================");
            System.out.println("Family Head: " + head.getFullName() + " (" + head.getNationalId() + ")");
            
            System.out.println("\nRegistered Members (" + members.size() + "):");
            for (int i = 0; i < members.size(); i++) {
                Citizen m = members.get(i);
                System.out.println(" - " + m.getFullName() + " [Role: " + m.getRelationToHead() + " | NID: " + m.getNationalId() + "]");
            }

            System.out.println("\n[Inferred Household Kinship Matrix]:");
            
            Citizen spouse = members.stream()
                .filter(m -> "SPOUSE".equalsIgnoreCase(m.getRelationToHead()))
                .findFirst().orElse(null);

            if (spouse != null) {
                System.out.println(" * " + spouse.getFullName() + " is married to Family Head " + head.getFullName());
            }

            List<Citizen> children = members.stream()
                .filter(m -> "CHILD".equalsIgnoreCase(m.getRelationToHead()))
                .toList();

            if (!children.isEmpty()) {
                System.out.println(" * Head (" + head.getFullName() + ")" + 
                    (spouse != null ? " and " + spouse.getFullName() : "") + 
                    " are parents to " + children.size() + " registered child(ren).");
                
                if (children.size() > 1) {
                    System.out.println(" * Inferred Sibling Matrix: Children are siblings to one another.");
                }
            }
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("Error fetching household data: " + e.getMessage());
        }
    }
    
    public static boolean citizenExists(Session session, String nationalId) {
        Long count = session.createQuery("SELECT COUNT(c) FROM Citizen c WHERE c.nationalId = :nid", Long.class)
                            .setParameter("nid", nationalId)
                            .uniqueResult();
        return count != null && count > 0;
    }
    
    public static void searchCitizenByName(SessionFactory sf, Scanner scanner) {
        try (Session session = sf.openSession()) {
            System.out.print("Enter name (or part of name) to search: ");
            String keyword = scanner.nextLine().trim();

            List<Citizen> results = session.createQuery(
                "FROM Citizen WHERE LOWER(fullName) LIKE :keyword", Citizen.class)
                .setParameter("keyword", "%" + keyword.toLowerCase())
                .getResultList();

            System.out.println("\n--- SEARCH RESULTS (" + results.size() + " found) ---");
            if (results.isEmpty()) {
                System.out.println("No citizens found matching that name.");
            } else {
                for (Citizen c : results) {
                    String headInfo = (c.getFamilyHead() != null) ? c.getFamilyHead().getFullName() : "Self (Head)";
                    System.out.println("Name: " + c.getFullName() + 
                                       " | NID: " + c.getNationalId() + 
                                       " | Relation to Head: " + c.getRelationToHead() + 
                                       " | Head: " + headInfo);
                }
            }
        } catch (Exception e) {
            System.out.println("Error searching citizens: " + e.getMessage());
        }
    }
    
    public static void viewFamilyAlliances(SessionFactory sf, Scanner scanner) {
        try (Session session = sf.openSession()) {
            System.out.print("Enter Family Head National ID to check alliances: ");
            String headNid = scanner.nextLine().trim();

            Citizen head = session.createQuery("FROM Citizen WHERE nationalId = :nid", Citizen.class)
                                  .setParameter("nid", headNid).uniqueResult();

            if (head == null) {
                System.out.println("Family Head not found.");
                return;
            }

            List<Relations> relations = session.createQuery(
                "FROM Relations WHERE familyAHead.nationalId = :nid OR familyBHead.nationalId = :nid", Relations.class)
                .setParameter("nid", headNid)
                .getResultList();

            System.out.println("\n--- FAMILY ALLIANCES FOR: " + head.getFullName() + " ---");
            if (relations.isEmpty()) {
                System.out.println("No registered inter-family alliances found.");
            } else {
                for (Relations rel : relations) {
                    Citizen alliedHead = rel.getFamilyAHead().getNationalId().equals(headNid) 
                                         ? rel.getFamilyBHead() 
                                         : rel.getFamilyAHead();
                    
                    System.out.println(" -> Allied Family Head: " + alliedHead.getFullName() + 
                                       " [NID: " + alliedHead.getNationalId() + 
                                       "] | Alliance Type: " + rel.getRelationType());
                }
            }
        } catch (Exception e) {
            System.out.println("Error fetching family alliances: " + e.getMessage());
        }
    }
   
    public static void analyzeKinship(SessionFactory sf, Scanner scanner) {
        try (Session session = sf.openSession()) {
            System.out.print("Enter National ID of Person 1: ");
            String nid1 = scanner.nextLine().trim();

            System.out.print("Enter National ID of Person 2: ");
            String nid2 = scanner.nextLine().trim();

            Citizen p1 = session.createQuery("FROM Citizen WHERE nationalId = :nid", Citizen.class)
                                .setParameter("nid", nid1).uniqueResult();
            Citizen p2 = session.createQuery("FROM Citizen WHERE nationalId = :nid", Citizen.class)
                                .setParameter("nid", nid2).uniqueResult();

            if (p1 == null || p2 == null) {
                System.out.println("One or both citizens could not be found.");
                return;
            }

            System.out.println("\n========================================");
            System.out.println(" KINSHIP ANALYSIS REPORT");
            System.out.println("========================================");
            System.out.println("Comparing: " + p1.getFullName() + " (" + p1.getNationalId() + ")");
            System.out.println("      and: " + p2.getFullName() + " (" + p2.getNationalId() + ")");
            System.out.println("----------------------------------------");

            // 1. Same Person Check
            if (p1.getNationalId().equals(p2.getNationalId())) {
                System.out.println("Result: This is the exact same person.");
                return;
            }

            // 2. Same Household Check
            boolean sameHousehold = false;
            String head1Nid = (p1.getFamilyHead() != null) ? p1.getFamilyHead().getNationalId() : p1.getNationalId();
            String head2Nid = (p2.getFamilyHead() != null) ? p2.getFamilyHead().getNationalId() : p2.getNationalId();

            if (head1Nid.equals(head2Nid)) {
                sameHousehold = true;
                System.out.println("Relationship: They belong to the SAME household (Head: " + head1Nid + ")");
                
                // Detailed intra-household logic
                if ("SELF".equals(p1.getRelationToHead()) && "CHILD".equals(p2.getRelationToHead())) {
                    System.out.println(" -> " + p1.getFullName()	 + " is the PARENT of " + p2.getFullName());
                } else if ("CHILD".equals(p1.getRelationToHead()) && "SELF".equals(p2.getRelationToHead())) {
                    System.out.println(" -> " + p2.getFullName() + " is the PARENT of " + p1.getFullName());
                } else if ("CHILD".equals(p1.getRelationToHead()) && "CHILD".equals(p2.getRelationToHead())) {
                    System.out.println(" -> Both are SIBLINGS (Children of the same Household Head).");
                } else if ("SPOUSE".equals(p1.getRelationToHead()) || "SPOUSE".equals(p2.getRelationToHead())) {
                    System.out.println(" -> They are married / spouses within the household.");
                } else {
                    System.out.println(" -> Extended household members under the same roof.");
                }
            } else {
                System.out.println("Relationship: They belong to DIFFERENT households.");
            }

            // 3. Cross-Family Alliance Check (Inter-Family Bridge)
            List<Relations> crossFamilyLinks = session.createQuery(
                "FROM Relations WHERE (familyAHead.nationalId = :h1 AND familyBHead.nationalId = :h2) " +
                "OR (familyAHead.nationalId = :h2 AND familyBHead.nationalId = :h1)", Relations.class)
                .setParameter("h1", head1Nid)
                .setParameter("h2", head2Nid)
                .getResultList();

            if (!crossFamilyLinks.isEmpty()) {
                System.out.println("Cross-Family Connection Found:");
                for (Relations r : crossFamilyLinks) {
                    System.out.println(" -> Their families are allied as: " + r.getRelationType() + 
                                       " (Linked via Family Heads " + r.getFamilyAHead().getFullName() + 
                                       " & " + r.getFamilyBHead().getFullName() + ")");
                }
            } else if (!sameHousehold) {
                System.out.println(" -> No direct inter-family alliance registered between their households yet.");
            }

            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("Error analyzing kinship: " + e.getMessage());
        }
    }
    
   
}