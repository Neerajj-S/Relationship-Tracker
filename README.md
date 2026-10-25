# Relationship Tracker

## Project Overview
The **Relationship Tracker** is a Spring Boot application designed to model, calculate, and visualize complex family structures, intra-household relationships, and inter-household alliances. The platform structures citizens into independent households headed by designated household heads and maps multi-generational blood and marital ties (such as siblings, spouses, children, in-laws, aunts, uncles, nieces, and nephews) across allied families.

---

## Future Updates & Roadmap
* **Web-Based Excel Data Import:** Transition data importing from the current Eclipse/IDE console runner to an intuitive file-upload interface directly on the webpage dashboard.
* **Role-Based Access Control (RBAC):** Enhance security and permissions so that only specific household heads or authorized members can modify alliances or split households.
* **Audit Logging & History:** Track historical lineage updates, household splits, and alliance creation dates within the MySQL database.
* **Advanced Kinship Graph Traversal:** Expand the relationship solver to automatically calculate multi-degree distant relatives (e.g., second cousins, grand-uncles, step-relatives) beyond direct household alliances.
