package org.relativesdb.connection;

import jakarta.servlet.http.HttpSession;
import org.relativesdb.connection.model.Citizen;
import org.relativesdb.connection.model.MemberKinshipDTO;
import org.relativesdb.connection.model.Relations;
import org.relativesdb.connection.repository.CitizenRepository;
import org.relativesdb.connection.repository.RelationsRepository;
import org.relativesdb.connection.service.KinshipService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class WebController {

    private final CitizenRepository citizenRepository;
    private final RelationsRepository relationsRepository;
    private final KinshipService kinshipService;

    public WebController(CitizenRepository citizenRepository, 
                         RelationsRepository relationsRepository, 
                         KinshipService kinshipService) {
        this.citizenRepository = citizenRepository;
        this.relationsRepository = relationsRepository;
        this.kinshipService = kinshipService;
    }

    // ==========================================
    // 1. LOGIN & SESSION ROUTES
    // ==========================================
    @GetMapping({"/", "/login"})
    public String loginPage(Model model, HttpSession session) {
        if (session.getAttribute("loggedInUserNationalId") != null) {
            return "redirect:/relations-table";
        }
        model.addAttribute("citizens", citizenRepository.findAll());
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String nationalId, HttpSession session) {
        if (nationalId != null && !nationalId.isBlank()) {
            session.setAttribute("loggedInUserNationalId", nationalId);
        }
        return "redirect:/relations-table";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ==========================================
    // 2. NETWORK VISUALIZER GRAPH
    // ==========================================
    @GetMapping("/family-network")
    public String viewFamilyNetwork(HttpSession session, Model model) {
        String loggedInId = (String) session.getAttribute("loggedInUserNationalId");
        if (loggedInId == null) return "redirect:/login";

        List<Citizen> allCitizens = citizenRepository.findAll();
        List<Relations> allRelations = relationsRepository.findAll();

        Citizen currentUser = allCitizens.stream()
                .filter(c -> c != null && loggedInId.equals(c.getNationalId()))
                .findFirst()
                .orElse(null);

        boolean isHead = kinshipService.isHouseholdHead(loggedInId);

        List<Citizen> filteredCitizens = new ArrayList<>();
        List<Relations> filteredRelations = new ArrayList<>();

        if (currentUser != null) {
            Citizen householdHead = "SELF".equalsIgnoreCase(currentUser.getRelationToHead()) || 
                                     (currentUser.getRelationToHead() != null && currentUser.getRelationToHead().toUpperCase().contains("HEAD"))
                                     ? currentUser 
                                     : currentUser.getFamilyHead();

            if (householdHead == null) {
                householdHead = currentUser;
            }

            final String headNid = householdHead.getNationalId();

            List<Citizen> ownHouseholdMembers = allCitizens.stream()
                    .filter(c -> c != null && (
                            c.getNationalId().equals(headNid) || 
                            (c.getFamilyHead() != null && headNid.equals(c.getFamilyHead().getNationalId()))
                    ))
                    .toList();
            filteredCitizens.addAll(ownHouseholdMembers);

            List<Relations> relevantRelations = allRelations.stream()
                    .filter(r -> r != null && r.getFamilyAHead() != null && r.getFamilyBHead() != null &&
                            (r.getFamilyAHead().getNationalId().equals(headNid) || 
                             r.getFamilyBHead().getNationalId().equals(headNid)))
                    .toList();
            filteredRelations.addAll(relevantRelations);

            for (Relations rel : relevantRelations) {
                Citizen alliedHead = rel.getFamilyAHead().getNationalId().equals(headNid) 
                        ? rel.getFamilyBHead() 
                        : rel.getFamilyAHead();

                if (alliedHead != null) {
                    String alliedHeadNid = alliedHead.getNationalId();
                    List<Citizen> alliedHouseholdMembers = allCitizens.stream()
                            .filter(c -> c != null && (
                                    c.getNationalId().equals(alliedHeadNid) || 
                                    (c.getFamilyHead() != null && alliedHeadNid.equals(c.getFamilyHead().getNationalId()))
                            ))
                            .toList();
                    
                    for (Citizen ac : alliedHouseholdMembers) {
                        if (filteredCitizens.stream().noneMatch(fc -> fc.getNationalId().equals(ac.getNationalId()))) {
                            filteredCitizens.add(ac);
                        }
                    }
                }
            }
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isHead", isHead);
        model.addAttribute("citizens", filteredCitizens);
        model.addAttribute("relations", filteredRelations);
        
        return "network";
    }

    // ==========================================
    // 3. BIDIRECTIONAL KINSHIP MATRIX TABLE (Split Perspective)
    // ==========================================
    @GetMapping("/relations-table")
    public String viewRelationsTable(HttpSession session, Model model) {
        String loggedInId = (String) session.getAttribute("loggedInUserNationalId");
        if (loggedInId == null) {
            return "redirect:/login";
        }

        Citizen currentUser = citizenRepository.findAll().stream()
                .filter(c -> c != null && loggedInId.equals(c.getNationalId()))
                .findFirst()
                .orElse(null);

        if (currentUser == null) {
            return "redirect:/logout";
        }

        boolean isHead = kinshipService.isHouseholdHead(loggedInId);

        // Split into internal household members and allied relatives
        List<MemberKinshipDTO> internalMatrix = kinshipService.getInternalKinshipForCitizen(loggedInId);
        List<MemberKinshipDTO> alliedMatrix = kinshipService.getAlliedKinshipForCitizen(loggedInId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isHead", isHead);
        model.addAttribute("internalMatrix", internalMatrix);
        model.addAttribute("alliedMatrix", alliedMatrix);
        
        return "relations-table";
    }

    // ==========================================
    // 4. HOUSEHOLD MANAGEMENT (WEB DASHBOARD)
    // ==========================================
    @GetMapping("/manage-households")
    public String manageHouseholds(HttpSession session, Model model) {
        String loggedInId = (String) session.getAttribute("loggedInUserNationalId");
        if (loggedInId == null || !kinshipService.isHouseholdHead(loggedInId)) {
            return "redirect:/relations-table";
        }

        Citizen currentUser = citizenRepository.findAll().stream()
                .filter(c -> c != null && loggedInId.equals(c.getNationalId()))
                .findFirst()
                .orElse(null);

        List<Citizen> familyHeads = citizenRepository.findAll().stream()
                .filter(c -> "SELF".equalsIgnoreCase(c.getRelationToHead()))
                .toList();
        
        List<Citizen> promotableMembers = citizenRepository.findAll().stream()
                .filter(c -> !"SELF".equalsIgnoreCase(c.getRelationToHead()))
                .toList();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isHead", true);
        model.addAttribute("familyHeads", familyHeads);
        model.addAttribute("promotableMembers", promotableMembers);
        return "manage-households";
    }

    // ==========================================
    // 5. LINK ALLIANCES FORM
    // ==========================================
    @GetMapping("/link-alliances")
    public String linkAlliances(HttpSession session, Model model) {
        String loggedInId = (String) session.getAttribute("loggedInUserNationalId");
        if (loggedInId == null || !kinshipService.isHouseholdHead(loggedInId)) {
            return "redirect:/relations-table";
        }

        Citizen currentUser = citizenRepository.findAll().stream()
                .filter(c -> c != null && loggedInId.equals(c.getNationalId()))
                .findFirst()
                .orElse(null);

        List<Citizen> allCitizens = citizenRepository.findAll();

        List<Citizen> familyHeads = allCitizens.stream()
                .filter(c -> c != null && ("SELF".equalsIgnoreCase(c.getRelationToHead()) || (c.getRelationToHead() != null && c.getRelationToHead().toUpperCase().contains("HEAD"))))
                .toList();
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isHead", true);
        model.addAttribute("familyHeads", familyHeads);
        model.addAttribute("allCitizens", allCitizens);
        return "link-alliances";
    }

    // ==========================================
    // 6. POST ACTIONS & FORM SUBMISSIONS
    // ==========================================

    @PostMapping("/api/households/add-member")
    public String addMember(@RequestParam String nationalId,
                            @RequestParam String fullName,
                            @RequestParam String relationToHead,
                            @RequestParam(required = false) String familyHeadId) {
        Citizen citizen = new Citizen();
        citizen.setNationalId(nationalId);
        citizen.setFullName(fullName);
        citizen.setRelationToHead(relationToHead);

        if (familyHeadId != null && !familyHeadId.isBlank()) {
            Citizen head = findCitizenByIdOrNid(familyHeadId);
            if (head != null) {
                citizen.setFamilyHead(head);
            }
        }

        citizenRepository.save(citizen);
        return "redirect:/manage-households";
    }

    @PostMapping("/api/households/promote-child")
    public String promoteChild(@RequestParam String childNid) {
        Citizen child = findCitizenByIdOrNid(childNid);
        if (child != null) {
            child.setRelationToHead("SELF");
            child.setFamilyHead(null);
            citizenRepository.save(child);
        }
        return "redirect:/manage-households";
    }

    @PostMapping("/api/alliances/link")
    public String linkAlliancesPost(@RequestParam String headANid,
                                    @RequestParam String headBNid,
                                    @RequestParam String relationType,
                                    @RequestParam(required = false) String siblingMemberId) {
        Citizen headA = findCitizenByIdOrNid(headANid);
        Citizen headB = findCitizenByIdOrNid(headBNid);

        if (headA != null && headB != null && !headANid.equals(headBNid)) {
            Relations relation = new Relations();
            relation.setFamilyAHead(headA);
            relation.setFamilyBHead(headB);
            relation.setRelationType(relationType);

            if (siblingMemberId != null && !siblingMemberId.isBlank()) {
                Citizen sibling = findCitizenByIdOrNid(siblingMemberId);
                relation.setSiblingMember(sibling);
            }

            relationsRepository.save(relation);
        }
        return "redirect:/relations-table";
    }
    
    // ==========================================
    //7.Transfer Member
    // ==========================================
    @PostMapping("/api/households/transfer-member")
    public String transferMember(@RequestParam String memberNid,
                                 @RequestParam String newHeadNid,
                                 @RequestParam String newRelationToHead) {
        Citizen member = findCitizenByIdOrNid(memberNid);
        if (member != null && newHeadNid != null && !newHeadNid.isBlank()) {
            Citizen newHead = findCitizenByIdOrNid(newHeadNid);
            if (newHead != null) {
                member.setFamilyHead(newHead);
               
                member.setRelationToHead(newRelationToHead != null && !newRelationToHead.isBlank() ? newRelationToHead : "CHILD");
                citizenRepository.save(member);
            }
        }
        return "redirect:/manage-households";
    }
    
    

    // ==========================================
    // 8. HELPER METHOD
    // ==========================================
    private Citizen findCitizenByIdOrNid(String idOrNid) {
        try {
            Long id = Long.parseLong(idOrNid);
            return citizenRepository.findById(id).orElseGet(() -> {
                return citizenRepository.findAll().stream()
                        .filter(c -> idOrNid.equals(c.getNationalId()))
                        .findFirst()
                        .orElse(null);
            });
        } catch (NumberFormatException e) {
            return citizenRepository.findAll().stream()
                    .filter(c -> idOrNid.equals(c.getNationalId()))
                    .findFirst()
                    .orElse(null);
        }
    }
}