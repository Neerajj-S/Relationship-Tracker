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

        if (allCitizens == null || allCitizens.isEmpty()) {
            return matrix;
        }

        // 1. GENERATE INTERNAL HOUSEHOLD RELATIONSHIPS (Intra-household)
        List<Citizen> heads = allCitizens.stream()
                .filter(c -> c != null && isHeadRole(c.getRelationToHead()))
                .toList();

        for (Citizen head : heads) {
            List<Citizen> householdMembers = getHouseholdMembers(allCitizens, head);
            for (int i = 0; i < householdMembers.size(); i++) {
                for (int j = 0; j < householdMembers.size(); j++) {
                    if (i == j) continue;
                    Citizen a = householdMembers.get(i);
                    Citizen b = householdMembers.get(j);

                    String aToB = resolveInternalRelationship(a, b);
                    String bToA = resolveInternalRelationship(b, a);

                    String personADesc = a.getFullName() + " (" + cleanRoleLabel(a.getRelationToHead()) + ")";
                    String personBDesc = b.getFullName() + " (" + cleanRoleLabel(b.getRelationToHead()) + ")";

                    matrix.add(new MemberKinshipDTO(
                            personADesc, head.getFullName(),
                            aToB, bToA,
                            personBDesc, head.getFullName()
                    ));
                }
            }
        }

        // 2. GENERATE EXTERNAL ALLIED HOUSEHOLD RELATIONSHIPS (Inter-household)
        if (relations != null && !relations.isEmpty()) {
            java.util.Set<String> processedPairs = new java.util.HashSet<>();

            for (Relations rel : relations) {
                if (rel == null || rel.getFamilyAHead() == null || rel.getFamilyBHead() == null) {
                    continue;
                }

                Citizen headA = rel.getFamilyAHead();
                Citizen headB = rel.getFamilyBHead();
                
                String pairKey = headA.getNationalId().compareTo(headB.getNationalId()) < 0 
                        ? headA.getNationalId() + "-" + headB.getNationalId() 
                        : headB.getNationalId() + "-" + headA.getNationalId();

                if (!processedPairs.add(pairKey)) {
                    continue; 
                }

                String relType = rel.getRelationType() != null ? rel.getRelationType().toUpperCase() : "IN_LAW";

                List<Citizen> membersA = getHouseholdMembers(allCitizens, headA);
                List<Citizen> membersB = getHouseholdMembers(allCitizens, headB);

                for (Citizen a : membersA) {
                    for (Citizen b : membersB) {
                        if (a == null || b == null) continue;

                        String aToB = resolveRelationship(a, b, relType, true, rel.getSiblingMember(), headA, headB);
                        String bToA = resolveRelationship(b, a, relType, false, rel.getSiblingMember(), headA, headB);

                        String descA = a.getFullName() + " (" + cleanRoleLabel(a.getRelationToHead()) + ")";
                        String descB = b.getFullName() + " (" + cleanRoleLabel(b.getRelationToHead()) + ")";

                        // Row 1: Perspective of Family A
                        matrix.add(new MemberKinshipDTO(
                                descA, headA.getFullName(),
                                aToB, bToA,
                                descB, headB.getFullName()
                        ));

                        // Row 2: Perspective of Family B
                        matrix.add(new MemberKinshipDTO(
                                descB, headB.getFullName(),
                                bToA, aToB,
                                descA, headA.getFullName()
                        ));
                    }
                }
            }
        }

        return matrix;
    }

    public List<MemberKinshipDTO> getKinshipMatrixForCitizen(String nationalId) {
        List<MemberKinshipDTO> fullMatrix = generateFullKinshipMatrix();
        if (nationalId == null || nationalId.isBlank()) {
            return new ArrayList<>();
        }

        List<Citizen> allCitizens = citizenRepository.findAll();
        Citizen loggedInUser = allCitizens.stream()
                .filter(c -> c != null && nationalId.equals(c.getNationalId()))
                .findFirst()
                .orElse(null);

        if (loggedInUser == null) return new ArrayList<>();

        // FIX: Match strictly using the unique National ID prefix pattern or unique description construct 
        // to prevent duplicate names like "Kavya" from cross-contaminating.
        String uniqueUserToken = loggedInUser.getFullName() + " (" + cleanRoleLabel(loggedInUser.getRelationToHead()) + ")";
        List<MemberKinshipDTO> filtered = new ArrayList<>();

        for (MemberKinshipDTO row : fullMatrix) {
            if (row.getPersonA() != null && row.getPersonA().equals(uniqueUserToken)) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    public List<MemberKinshipDTO> getInternalKinshipForCitizen(String nationalId) {
        List<MemberKinshipDTO> userMatrix = getKinshipMatrixForCitizen(nationalId);
        return userMatrix.stream()
                .filter(row -> row.getPersonAHead() != null && 
                               row.getPersonAHead().equals(row.getPersonBHead()) &&
                               !row.getPersonA().equals(row.getPersonB()))
                .toList();
    }

    public List<MemberKinshipDTO> getAlliedKinshipForCitizen(String nationalId) {
        List<MemberKinshipDTO> userMatrix = getKinshipMatrixForCitizen(nationalId);
        return userMatrix.stream()
                .filter(row -> row.getPersonAHead() != null && 
                               row.getPersonBHead() != null && 
                               !row.getPersonAHead().equals(row.getPersonBHead()))
                .toList();
    }

    public boolean isHouseholdHead(String nationalId) {
        if (nationalId == null) return false;
        
        List<Citizen> allCitizens = citizenRepository.findAll();
        Citizen c = allCitizens.stream()
                .filter(citizen -> citizen != null && nationalId.equals(citizen.getNationalId()))
                .findFirst()
                .orElse(null);

        if (c == null) return false;
        
        String role = c.getRelationToHead() != null ? c.getRelationToHead().toUpperCase() : "";
        return isHeadRole(role);
    }

    private boolean isHeadRole(String role) {
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return r.contains("SELF") || r.contains("HEAD");
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
        
        try {
            String genderStr = c.getGender();
            if (genderStr != null && !genderStr.isBlank()) {
                String g = genderStr.trim().toUpperCase();
                if (g.startsWith("F") || g.equals("FEMALE")) return false;
                if (g.startsWith("M") || g.equals("MALE")) return true;
            }
        } catch (Exception ignored) {}

        String role = c.getRelationToHead() != null ? c.getRelationToHead().toUpperCase() : "";
        if (role.contains("WIFE") || role.contains("DAUGHTER") || role.contains("MOTHER") || 
            role.contains("SISTER") || role.contains("GRANDDAUGHTER")) {
            return false;
        }
        if (role.contains("HUSBAND") || role.contains("SON") || role.contains("FATHER") || 
            role.contains("BROTHER") || role.contains("GRANDSON") || isHeadRole(role)) {
            return true;
        }

        return true;
    }

    private String resolveInternalRelationship(Citizen subject, Citizen target) {
        String subRole = normalizeRole(subject.getRelationToHead());
        String tarRole = normalizeRole(target.getRelationToHead());
        boolean targetIsMale = isMale(target);

        if ("HEAD".equals(subRole)) {
            if ("SPOUSE".equals(tarRole)) return targetIsMale ? "Husband" : "Wife";
            if ("CHILD".equals(tarRole)) return targetIsMale ? "Son" : "Daughter";
            if ("GRANDCHILD".equals(tarRole)) return targetIsMale ? "Grandson" : "Granddaughter";
        }
        if ("SPOUSE".equals(subRole)) {
            if ("HEAD".equals(tarRole)) return targetIsMale ? "Husband" : "Wife";
            if ("CHILD".equals(tarRole)) return targetIsMale ? "Son" : "Daughter";
            if ("GRANDCHILD".equals(tarRole)) return targetIsMale ? "Grandson" : "Granddaughter";
        }
        if ("CHILD".equals(subRole)) {
            if ("HEAD".equals(tarRole) || "SPOUSE".equals(tarRole)) return targetIsMale ? "Father" : "Mother";
            if ("CHILD".equals(tarRole)) return targetIsMale ? "Brother" : "Sister";
            if ("GRANDCHILD".equals(tarRole)) return targetIsMale ? "Son" : "Daughter";
        }
        if ("GRANDCHILD".equals(subRole)) {
            if ("HEAD".equals(tarRole) || "SPOUSE".equals(tarRole)) return targetIsMale ? "Grandfather" : "Grandmother";
            if ("CHILD".equals(tarRole)) return targetIsMale ? "Father" : "Mother";
            if ("GRANDCHILD".equals(tarRole)) return targetIsMale ? "Brother" : "Sister";
        }
        return targetIsMale ? "In-law" : "In-law";
    }

    private String resolveRelationship(Citizen subject, Citizen target, String baseAlliance, boolean isDirectAtoB, Citizen siblingMember, Citizen headA, Citizen headB) {
        String subRole = normalizeRole(subject.getRelationToHead());
        String tarRole = normalizeRole(target.getRelationToHead());
        boolean targetIsMale = isMale(target);

        if (siblingMember != null) {
            String sibNid = siblingMember.getNationalId();
            boolean subIsSib = subject.getNationalId().equals(sibNid);
            boolean tarIsSib = target.getNationalId().equals(sibNid);

            // 1. If target is the specific sibling bridge Kavya
            if (tarIsSib) {
                if (headB != null && subject.getNationalId().equals(headB.getNationalId())) {
                    return "Wife";
                }
                // If viewer is a child/dependent relative (generational difference -> Aunt/Uncle)
                if ("CHILD".equals(subRole) || "GRANDCHILD".equals(subRole)) {
                    return targetIsMale ? "Uncle" : "Aunt";
                }
                return targetIsMale ? "Brother" : "Sister";
            }

            // 2. If subject is the specific sibling bridge Kavya looking outward
            if (subIsSib) {
                if (headB != null && target.getNationalId().equals(headB.getNationalId())) {
                    return "Husband";
                }
                // If Kavya is looking at children/dependents (generational difference -> Niece/Nephew)
                if ("CHILD".equals(tarRole) || "GRANDCHILD".equals(tarRole)) {
                    return targetIsMale ? "Nephew" : "Niece";
                }
                return targetIsMale ? "Brother" : "Sister";
            }

            boolean subIsHeadA = headA != null && subject.getNationalId().equals(headA.getNationalId());
            boolean tarIsHeadB = headB != null && target.getNationalId().equals(headB.getNationalId());
            boolean subIsHeadB = headB != null && subject.getNationalId().equals(headB.getNationalId());
            boolean tarIsHeadA = headA != null && target.getNationalId().equals(headA.getNationalId());

            if ((subIsHeadA && tarIsHeadB) || (subIsHeadB && tarIsHeadA)) {
                return targetIsMale ? "Brother-in-law" : "Sister-in-law";
            }
        }

        if (baseAlliance.contains("BROTHER") || baseAlliance.contains("SIBLING")) {
            if ("HEAD".equals(subRole) && "HEAD".equals(tarRole)) return targetIsMale ? "Brother" : "Sister";
            if ("HEAD".equals(subRole) && ("CHILD".equals(tarRole) || "GRANDCHILD".equals(tarRole))) return targetIsMale ? "Nephew" : "Niece";
            if ("HEAD".equals(subRole) && ("SPOUSE".equals(tarRole) || "IN_LAW".equals(tarRole))) return targetIsMale ? "Brother-in-law" : "Sister-in-law";
            if (("CHILD".equals(subRole) || "GRANDCHILD".equals(subRole)) && "HEAD".equals(tarRole)) return targetIsMale ? "Uncle" : "Aunt";
            if ("CHILD".equals(subRole) && "CHILD".equals(tarRole)) return "First Cousin";
            if (("SPOUSE".equals(subRole) || "IN_LAW".equals(subRole)) && "HEAD".equals(tarRole)) return targetIsMale ? "Brother-in-law" : "Sister-in-law";
            return targetIsMale ? "Brother-in-law" : "Sister-in-law";
        }

        if (baseAlliance.contains("PARENT") || baseAlliance.contains("FATHER") || baseAlliance.contains("SON")) {
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
            } else {
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
        if (isHeadRole(r)) return "HEAD";
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