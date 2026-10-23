package org.relativesdb.connection;

import org.relativesdb.connection.model.Citizen;
import org.relativesdb.connection.model.NetworkData;
import org.relativesdb.connection.model.Relations;
import org.relativesdb.connection.repository.CitizenRepository;
import org.relativesdb.connection.repository.RelationsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class WebController {

    @Autowired
    private CitizenRepository citizenRepository;

    @GetMapping("/citizens-page")
    public String viewCitizensPage(Model model) {
        
        List<Citizen> citizens = citizenRepository.findAll();
        
        model.addAttribute("citizensList", citizens);
        
        return "citizens"; 
    }
    
    @Autowired
    private RelationsRepository relationsRepository;

    @GetMapping("/api/network-data")
    @ResponseBody
    public NetworkData getNetworkData() {
        List<Citizen> allCitizens = citizenRepository.findAll();

        List<Citizen> heads = allCitizens.stream()
                .filter(c -> "SELF".equalsIgnoreCase(c.getRelationToHead()))
                .toList();

        List<Relations> allRelations = relationsRepository.findAll();

        List<NetworkData.Node> nodes = heads.stream().map(head -> {
            List<String> memberNames = allCitizens.stream()
                    .filter(c -> c.getFamilyHead() != null && 
                                 c.getFamilyHead().getNationalId().equals(head.getNationalId()))
                    .map(c -> "• " + c.getFullName() + " (" + c.getRelationToHead() + ")")
                    .toList();

            StringBuilder labelBuilder = new StringBuilder();
            labelBuilder.append("🏠 ").append(head.getFullName()).append(" (Head)\n");
            labelBuilder.append("-----------------------------\n");
            
            if (memberNames.isEmpty()) {
                labelBuilder.append("(No other members)");
            } else {
                labelBuilder.append(String.join("\n", memberNames));
            }

            return new NetworkData.Node(
                    head.getNationalId(),
                    labelBuilder.toString(), // Passed into the "label" field
                    "FamilyHead"
            );
        }).toList();

        List<NetworkData.Edge> edges = allRelations.stream()
                .map(r -> new NetworkData.Edge(
                        r.getFamilyAHead().getNationalId(),
                        r.getFamilyBHead().getNationalId(),
                        r.getRelationType()
                ))
                .toList();

        return new NetworkData(nodes, edges);
    }

    @GetMapping("/family-network")
    public String viewNetworkPage() {
        return "network"; // Will point to network.html
    }
}