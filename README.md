# Relationship Tracker

## Project Overview
The **Relationship Tracker** is a Spring Boot application designed to model, calculate, and visualize complex family structures, intra-household relationships, and inter-household alliances. The platform structures citizens into independent households headed by designated household heads and maps multi-generational blood and marital ties (such as siblings, spouses, children, in-laws, aunts, uncles, nieces, and nephews) across allied families.

---

## Current Focus & Recent Work
We have recently stabilized the core architecture around **independent household heads (Gaurav, Ajay, and Ganesh)** and perfected the **dual-perspective kinship matrix calculation**:

1. **Decoupled Internal & External Logic:** Cleanly separated intra-household family relationships from inter-household external alliances to prevent family member bleeding between tables.
2. **Independent Split Heads:** Successfully transitioned split family branches (like Ajay and his household) into autonomous nodes with their own unique leadership and dependent mapping.
3. **Bidirectional Row Generation:** Implemented a symmetric row-generation strategy (`generateFullKinshipMatrix`) ensuring that whichever family logs in (Family A or Family B), their respective external relationship views populate equally and accurately.
4. **Context-Aware Relationship Resolution:** Fixed edge cases involving sibling-bridge alliances (such as correctly separating duplicate names like "Kavya" into an aunt vs. a wife using role and ID validation) to accurately resolve generational titles like *Aunt/Niece* versus *Sister/Brother*.

---

## Future Updates & Roadmap
* **Web-Based Excel Data Import:** Transition data importing from the current Eclipse/IDE console runner to an intuitive file-upload interface directly on the webpage dashboard.
* **Role-Based Access Control (RBAC):** Enhance security and permissions so that only specific household heads or authorized members can modify alliances or split households.
* **Graphical Tree Visualization:** Integrate a front-end tree/graph rendering library (such as D3.js or Vis.js) to display the kinship network visually alongside tabular matrices.
* **Audit Logging & History:** Track historical lineage updates, household splits, and alliance creation dates within the MySQL database.
* **Advanced Kinship Graph Traversal:** Expand the relationship solver to automatically calculate multi-degree distant relatives (e.g., second cousins, grand-uncles, step-relatives) beyond direct household alliances.
