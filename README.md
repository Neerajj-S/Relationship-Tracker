# Relationship Tracker

**Relationship Tracker** is a dynamic, Spring Boot-based genealogy and kinship management system. It bridges the gap between traditional hierarchical family trees and modern, multi-household alliance networks by resolving bidirectional kinship semantics and providing interactive relationship visualization.

---

## 🚀 The Problem
Traditional genealogy systems often treat families as isolated, static trees, making it difficult to map complex multi-household alliances (such as inter-family marriages, senior-to-junior household branches, and collateral kinship). Furthermore, they usually lack role-scoped views, displaying global administrative data to all users alike. 

Relationship Tracker solves this by:
1. **Automating Bidirectional Kinship Resolution**: Dynamically calculating accurate, gender-aware relationship terms (e.g., Father-in-law, Uncle, Niece, First Cousin) from both perspectives across linked households.
2. **Session-Aware Role Scoping**: Enforcing security and context so that Household Heads have full administrative and alliance-management access, while regular members are restricted to viewing only their own personal relationship matrix and network perspective.

---

## 🛠️ Tech Stack
* **Backend**: Java, Spring Boot, Spring Data JPA
* **Database**: Relational Database (H2 / MySQL / PostgreSQL via JPA)
* **Frontend**: Thymeleaf, HTML5, CSS3 (Modern responsive UI)
* **Version Control**: Git & GitHub

---

## ✨ Key Features
* **Multi-Household Architecture**: Manage independent households with designated `Citizen` heads and members (`Relations`).
* **Dynamic Kinship Matrix**: Generates cross-family relationship tables with real-time gender and generational offset resolution.
* **Role-Based Access Control (Session-Aware)**: 
  * *Household Heads* (e.g., Gaurav): Full access to household management, alliance linking, and network visualizations.
  * *Regular Members* (e.g., Ramesh): Restricted view limited strictly to personal kinship perspectives.
* **Network Graph Visualization**: Interactive mapping of family alliances.

---

## 🔮 Future Updates
* **Extended Collateral Kin**: Support for automatic calculation of second and third cousins, as well as complex "removed" generational tiers (e.g., once removed).
* **Advanced Authentication**: Full Spring Security integration with encrypted passwords and granular user roles.
* **Export Utilities**: Export family matrices and network trees to PDF or CSV formats.

---
