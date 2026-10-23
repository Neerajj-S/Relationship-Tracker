package org.relativesdb.connection.service;

import org.relativesdb.connection.model.Citizen;
import org.relativesdb.connection.model.Relations;
import org.relativesdb.connection.repository.CitizenRepository;
import org.relativesdb.connection.repository.RelationsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Scanner;

@Service
public class CitizenService {

    @Autowired
    private CitizenRepository citizenRepository;

    @Autowired
    private RelationsRepository relationsRepository;



    // --- 1. Link Two Families (Inter-Family Alliance) ---
    public void linkFamilies(Scanner scanner) {
        System.out.print("Enter Family A Head National ID: ");
        String headAId = scanner.nextLine();

        System.out.print("Enter Family B Head National ID: ");
        String headBId = scanner.nextLine();

        Citizen headA = citizenRepository.findByNationalId(headAId);
        Citizen headB = citizenRepository.findByNationalId(headBId);

        if (headA != null && headB != null) {
            Relations relation = new Relations();
            relation.setFamilyAHead(headA);
            relation.setFamilyBHead(headB);

            System.out.print("Enter relationship type between families (e.g., IN_LAWS): ");
            relation.setRelationType(scanner.nextLine().toUpperCase());

            relationsRepository.save(relation);
            System.out.println("Families successfully linked!");
        } else {
            System.out.println("One or both Family Heads could not be found.");
        }
    }

    
    // --- 2. Search Citizen by Name ---
    public void searchCitizenByName(Scanner scanner) {
        System.out.print("Enter name (or part of name) to search: ");
        String keyword = scanner.nextLine().trim();

        List<Citizen> results = citizenRepository.findByFullNameContainingIgnoreCase(keyword);

        System.out.println("\n--- SEARCH RESULTS (" + results.size() + " found) ---");
        if (results.isEmpty()) {
            System.out.println("No citizens found matching that name.");
        } else {
            for (Citizen c : results) {
                String headInfo = (c.getFamilyHead() != null) ? c.getFamilyHead().getFullName() : "Self (Head)";
                System.out.println("Name: " + c.getFullName() + 
                                   " | NID: " + c.getNationalId() + 
                                   " | Relation: " + c.getRelationToHead() + 
                                   " | Head: " + headInfo);
            }
        }
    }
    
 // --- 3. Promote Existing Child to Independent Head (Option 7) ---
    public void promoteExistingChildToHead(Scanner scanner) {
        System.out.println("\n========================================");
        System.out.println(" PROMOTE EXISTING CHILD TO INDEPENDENT HEAD ");
        System.out.println("========================================");

        System.out.print("Enter the National ID of the child to promote: ");
        String nationalId = scanner.nextLine().trim();

        Citizen citizen = citizenRepository.findByNationalId(nationalId);

        if (citizen == null) {
            System.out.println("Error: No citizen found with National ID: " + nationalId);
            return;
        }

        System.out.println("Found: " + citizen.getFullName() + " (Current relation: " + citizen.getRelationToHead() + ")");

        // Promote to Head
        citizen.setRelationToHead("SELF");
        citizen.setFamilyHead(null); // No longer under another roof

        citizenRepository.save(citizen);
        System.out.println("Success! " + citizen.getFullName() + " is now promoted to an independent Family Head.");
    }
    
 // --- 4. Transfer Family Members to New Independent Head ---
    public void transferFamilyMembers(Scanner scanner) {
        System.out.println("\n========================================");
        System.out.println(" TRANSFER FAMILY MEMBERS TO NEW HEAD   ");
        System.out.println("========================================");

        System.out.print("Enter the New Family Head's National ID (e.g., Ajay): ");
        String newHeadNid = scanner.nextLine().trim();

        Citizen newHead = citizenRepository.findByNationalId(newHeadNid);
        if (newHead == null || !"SELF".equalsIgnoreCase(newHead.getRelationToHead())) {
            System.out.println("Error: No independent Family Head found with that National ID. Promote them to head first!");
            return;
        }

        System.out.println("-> Target Household Head Confirmed: " + newHead.getFullName());

        boolean transferring = true;
        while (transferring) {
            System.out.print("\nEnter National ID of family member to transfer (or type 'done' to finish): ");
            String memberNid = scanner.nextLine().trim();

            if (memberNid.equalsIgnoreCase("done")) {
                transferring = false;
                break;
            }

            Citizen member = citizenRepository.findByNationalId(memberNid);
            if (member == null) {
                System.out.println("Error: Citizen not found with National ID: " + memberNid);
                continue;
            }

            if (member.getNationalId().equals(newHead.getNationalId())) {
                System.out.println("Error: A family head cannot be transferred to themselves.");
                continue;
            }

            // Reassign to new head
            member.setFamilyHead(newHead);

            System.out.print("Enter their new relation to " + newHead.getFullName() + " (e.g., SPOUSE, CHILD): ");
            String relation = scanner.nextLine().trim().toUpperCase();
            if (!relation.isEmpty()) {
                member.setRelationToHead(relation);
            }

            citizenRepository.save(member);
            System.out.println("-> Success! " + member.getFullName() + " is now part of " + newHead.getFullName() + "'s household.");
        }
        System.out.println("Family member transfer session ended.");
    }
}