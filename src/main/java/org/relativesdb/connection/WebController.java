package org.relativesdb.connection;

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
    // 1. NETWORK VISUALIZER GRAPH
    // ==========================================
    @GetMapping("/family-network")
    public String viewFamilyNetwork(Model model) {
        List<Citizen> citizens = citizenRepository.findAll();
        List<Relations> relations = relationsRepository.findAll();
        
        model.addAttribute("citizens", citizens);
        model.addAttribute("relations", relations);
        return "network";
    }

    // ==========================================
    // 2. BIDIRECTIONAL KINSHIP MATRIX TABLE
    // ==========================================
    @GetMapping("/relations-table")
    public String viewRelationsTable(Model model) {
        List<MemberKinshipDTO> kinshipMatrix = kinshipService.generateFullKinshipMatrix();
        model.addAttribute("matrix", kinshipMatrix);
        return "relations-table";
    }

    // ==========================================
    // 3. HOUSEHOLD MANAGEMENT (WEB DASHBOARD)
    // ==========================================
    @GetMapping("/manage-households")
    public String manageHouseholds(Model model) {
        List<Citizen> familyHeads = citizenRepository.findAll().stream()
                .filter(c -> "SELF".equalsIgnoreCase(c.getRelationToHead()))
                .toList();
        
        List<Citizen> promotableMembers = citizenRepository.findAll().stream()
                .filter(c -> !"SELF".equalsIgnoreCase(c.getRelationToHead()))
                .toList();

        model.addAttribute("familyHeads", familyHeads);
        model.addAttribute("promotableMembers", promotableMembers);
        return "manage-households";
    }

    // ==========================================
    // 4. LINK ALLIANCES FORM
    // ==========================================
    @GetMapping("/link-alliances")
    public String linkAlliances(Model model) {
        List<Citizen> familyHeads = citizenRepository.findAll().stream()
                .filter(c -> "SELF".equalsIgnoreCase(c.getRelationToHead()))
                .toList();
        
        model.addAttribute("familyHeads", familyHeads);
        return "link-alliances";
    }

    // ==========================================
    // 5. POST ACTIONS & FORM SUBMISSIONS
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
                                    @RequestParam String relationType) {
        Citizen headA = findCitizenByIdOrNid(headANid);
        Citizen headB = findCitizenByIdOrNid(headBNid);

        if (headA != null && headB != null && !headANid.equals(headBNid)) {
            Relations relation = new Relations();
            relation.setFamilyAHead(headA);
            relation.setFamilyBHead(headB);
            relation.setRelationType(relationType);
            relationsRepository.save(relation);
        }
        return "redirect:/relations-table";
    }

    // ==========================================
    // 6. HELPER METHOD
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