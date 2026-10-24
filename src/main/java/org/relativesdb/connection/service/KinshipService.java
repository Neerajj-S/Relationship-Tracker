package org.relativesdb.connection.service;

import org.relativesdb.connection.model.Citizen;
import org.relativesdb.connection.model.MemberKinshipDTO;
import org.relativesdb.connection.model.Relations;
import org.relativesdb.connection.repository.CitizenRepository;
import org.relativesdb.connection.repository.RelationsRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KinshipService {

    private final CitizenRepository citizenRepository;
    private final RelationsRepository relationsRepository;

    public KinshipService(CitizenRepository citizenRepository, RelationsRepository relationsRepository) {
        this.citizenRepository = citizenRepository;
        this.relationsRepository = relationsRepository;
    }

    public List<MemberKinshipDTO> generateFullKinshipMatrix() {
        List<MemberKinshipDTO> matrix = new ArrayList<>();
        List<Citizen> allCitizens = citizenRepository.findAll();
        List<Relations> relations = relationsRepository.findAll();

        if (relations == null || relations.isEmpty()) {
            return matrix;
        }

        for (Relations rel : relations) {
            if (rel == null || rel.getFamilyAHead() == null || rel.getFamilyBHead() == null) {
                continue;
            }

            Citizen headA = rel.getFamilyAHead();
            Citizen headB = rel.getFamilyBHead();
            String relType = rel.getRelationType() != null ? rel.getRelationType().toUpperCase() : "PARENT_CHILD";

            List<Citizen> membersA = getHouseholdMembers(allCitizens, headA);
            List<Citizen> membersB = getHouseholdMembers(allCitizens, headB);

            for (Citizen a : membersA) {
                for (Citizen b : membersB) {
                    if (a == null || b == null) continue;

                    String aToB = resolveRelationship(a, b, relType, true);
                    String bToA = resolveRelationship(b, a, relType, false);

                    String personADesc = a.getFullName() + " (" + cleanRoleLabel(a.getRelationToHead()) + ")";
                    String personBDesc = b.getFullName() + " (" + cleanRoleLabel(b.getRelationToHead()) + ")";

                    matrix.add(new MemberKinshipDTO(
                            personADesc, headA.getFullName(),
                            aToB, bToA,
                            personBDesc, headB.getFullName()
                    ));
                }
            }
        }
        return matrix;
    }

    private List<Citizen> getHouseholdMembers(List<Citizen> all, Citizen head) {
        if (all == null || head == null) return new ArrayList<>();
        return all.stream()
                .filter(c -> c != null && (
                             c.getNationalId().equals(head.getNationalId()) || 
                             (c.getFamilyHead() != null && head.getNationalId().equals(c.getFamilyHead().getNationalId()))
                ))
                .toList();
    }

    private boolean isMale(Citizen c) {
        if (c == null) return true;
        
        // 1. Check direct gender property on Citizen
        try {
            String genderStr = null;
            try {
                java.lang.reflect.Method m = c.getClass().getMethod("getGender");
                Object val = m.invoke(c);
                if (val != null) genderStr = val.toString().trim().toUpperCase();
            } catch (Exception ignored) {}

            if (genderStr != null && !genderStr.isBlank()) {
                if (genderStr.startsWith("F")) return false;
                if (genderStr.startsWith("M")) return true;
            }
        } catch (Exception ignored) {}

        // 2. Check role-to-head keywords
        String role = c.getRelationToHead() != null ? c.getRelationToHead().toUpperCase() : "";
        if (role.contains("WIFE") || role.contains("DAUGHTER") || role.contains("MOTHER") || 
            role.contains("SISTER") || role.contains("GRANDDAUGHTER")) {
            return false;
        }
        if (role.contains("HUSBAND") || role.contains("SON") || role.contains("FATHER") || 
            role.contains("BROTHER") || role.contains("GRANDSON") || role.contains("SELF") || role.contains("HEAD")) {
            return true;
        }

        return true; // Default fallback
    }

    private String resolveRelationship(Citizen subject, Citizen target, String baseAlliance, boolean isDirectAtoB) {
        String subRole = normalizeRole(subject.getRelationToHead());
        String tarRole = normalizeRole(target.getRelationToHead());
        boolean targetIsMale = isMale(target); // KEY FIX: Kinship terms describe the TARGET, so gender depends on target!

        // ----------------------------------------------------
        // 1. BROTHERS / SIBLINGS ALLIANCE
        // ----------------------------------------------------
        if (baseAlliance.contains("BROTHER") || baseAlliance.contains("SIBLING")) {
            if ("HEAD".equals(subRole) && "HEAD".equals(tarRole)) return targetIsMale ? "Brother" : "Sister";
            if ("HEAD".equals(subRole) && ("CHILD".equals(tarRole) || "GRANDCHILD".equals(tarRole))) return targetIsMale ? "Nephew" : "Niece";
            if ("HEAD".equals(subRole) && ("SPOUSE".equals(tarRole) || "IN_LAW".equals(tarRole))) return targetIsMale ? "Brother-in-law" : "Sister-in-law";
            if (("CHILD".equals(subRole) || "GRANDCHILD".equals(subRole)) && "HEAD".equals(tarRole)) return targetIsMale ? "Uncle" : "Aunt";
            if (("CHILD".equals(subRole) || "GRANDCHILD".equals(subRole)) && ("SPOUSE".equals(tarRole) || "IN_LAW".equals(tarRole))) return targetIsMale ? "Uncle-in-law" : "Aunt-in-law";
            if ("CHILD".equals(subRole) && "CHILD".equals(tarRole)) return "First Cousin";
            if (("SPOUSE".equals(subRole) || "IN_LAW".equals(subRole)) && "HEAD".equals(tarRole)) return targetIsMale ? "Brother-in-law" : "Sister-in-law";
            if (("SPOUSE".equals(subRole) || "IN_LAW".equals(subRole)) && "SPOUSE".equals(tarRole)) return "In-law";
            return "Collateral Kin";
        }

        // ----------------------------------------------------
        // 2. PARENT_CHILD ALLIANCE (Senior Household A <-> Junior Household B)
        // ----------------------------------------------------
        if (baseAlliance.contains("PARENT") || baseAlliance.contains("FATHER") || baseAlliance.contains("SON")) {
            
            // Perspective A -> B (Senior Family to Junior Family)
            if (isDirectAtoB) {
                if ("HEAD".equals(subRole)) {
                    if ("HEAD".equals(tarRole)) return targetIsMale ? "Son" : "Daughter";
                    if ("SPOUSE".equals(tarRole) || "IN_LAW".equals(tarRole)) return targetIsMale ? "Son-in-law" : "Daughter-in-law";
                    if ("CHILD".equals(tarRole) || "GRANDCHILD".equals(tarRole)) return "Grandchild";
                }
                if ("SPOUSE".equals(subRole)) {
                    if ("HEAD".equals(tarRole)) return targetIsMale ? "Son" : "Daughter";
                    if ("SPOUSE".equals(tarRole) || "IN_LAW".equals(tarRole)) return targetIsMale ? "Son-in-law" : "Daughter-in-law";
                    if ("CHILD".equals(tarRole) || "GRANDCHILD".equals(tarRole)) return "Grandchild";
                }
                if ("CHILD".equals(subRole)) {
                    if ("HEAD".equals(tarRole)) return targetIsMale ? "Brother" : "Sister";
                    if ("SPOUSE".equals(tarRole) || "IN_LAW".equals(tarRole)) return targetIsMale ? "Brother-in-law" : "Sister-in-law";
                    if ("CHILD".equals(tarRole) || "GRANDCHILD".equals(tarRole)) return targetIsMale ? "Nephew" : "Niece";
                }
                if ("IN_LAW".equals(subRole)) {
                    if ("HEAD".equals(tarRole)) return targetIsMale ? "Brother-in-law" : "Sister-in-law";
                    if ("SPOUSE".equals(tarRole) || "IN_LAW".equals(tarRole)) return "In-law";
                    if ("CHILD".equals(tarRole) || "GRANDCHILD".equals(tarRole)) return targetIsMale ? "Nephew-in-law" : "Niece-in-law";
                }
                if ("GRANDCHILD".equals(subRole)) {
                    if ("HEAD".equals(tarRole)) return targetIsMale ? "Uncle" : "Aunt";
                    if ("SPOUSE".equals(tarRole) || "IN_LAW".equals(tarRole)) return targetIsMale ? "Uncle-in-law" : "Aunt-in-law";
                    if ("CHILD".equals(tarRole) || "GRANDCHILD".equals(tarRole)) return "First Cousin";
                }
            } 
            // Perspective B -> A (Junior Family to Senior Family)
            else {
                if ("HEAD".equals(subRole)) {
                    if ("HEAD".equals(tarRole) || "SPOUSE".equals(tarRole)) return targetIsMale ? "Father" : "Mother";
                    if ("CHILD".equals(tarRole) || "IN_LAW".equals(tarRole)) return targetIsMale ? "Brother" : "Sister";
                    if ("GRANDCHILD".equals(tarRole)) return targetIsMale ? "Nephew" : "Niece";
                }
                if ("SPOUSE".equals(subRole) || "IN_LAW".equals(subRole)) {
                    if ("HEAD".equals(tarRole) || "SPOUSE".equals(tarRole)) return targetIsMale ? "Father-in-law" : "Mother-in-law";
                    if ("CHILD".equals(tarRole) || "IN_LAW".equals(tarRole)) return targetIsMale ? "Brother-in-law" : "Sister-in-law";
                    if ("GRANDCHILD".equals(tarRole)) return targetIsMale ? "Nephew-in-law" : "Niece-in-law";
                }
                if ("CHILD".equals(subRole) || "GRANDCHILD".equals(subRole)) {
                    if ("HEAD".equals(tarRole) || "SPOUSE".equals(tarRole)) return targetIsMale ? "Grandfather" : "Grandmother";
                    if ("CHILD".equals(tarRole) || "IN_LAW".equals(tarRole)) return targetIsMale ? "Uncle" : "Aunt";
                    if ("GRANDCHILD".equals(tarRole)) return "First Cousin";
                }
            }
        }

        return "In-law";
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null) return "CHILD";
        String r = rawRole.trim().toUpperCase();
        if (r.contains("SELF") || r.contains("HEAD")) return "HEAD";
        if (r.contains("SPOUSE") || r.contains("WIFE") || r.contains("HUSBAND")) return "SPOUSE";
        if (r.contains("IN_LAW") || r.contains("IN LAW")) return "IN_LAW";
        if (r.contains("GRANDCHILD") || r.contains("GRANDSON") || r.contains("GRANDDAUGHTER")) return "GRANDCHILD";
        if (r.contains("CHILD") || r.contains("SON") || r.contains("DAUGHTER")) return "CHILD";
        return "CHILD";
    }

    private String cleanRoleLabel(String rawRole) {
        if (rawRole == null) return "Member";
        return rawRole.replace("_", " ").toLowerCase();
    }
}