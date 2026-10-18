package com.project.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


@Entity
@Table(name = "family_relations")
public class Relations {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Points to the Head of Family A
    @ManyToOne
    @JoinColumn(name = "family_a_head_national_id", referencedColumnName = "national_id")
    private Citizen familyAHead;

    // Points to the Head of Family B
    @ManyToOne
    @JoinColumn(name = "family_b_head_national_id", referencedColumnName = "national_id")
    private Citizen familyBHead;

    @Column(name = "relation_type")
    private String relationType; // e.g., "IN_LAWS", "ALLIED_FAMILY", "EXTENDED"

    // Default Constructor
    public Relations() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Citizen getFamilyAHead() {
        return familyAHead;
    }

    public void setFamilyAHead(Citizen familyAHead) {
        this.familyAHead = familyAHead;
    }

    public Citizen getFamilyBHead() {
        return familyBHead;
    }

    public void setFamilyBHead(Citizen familyBHead) {
        this.familyBHead = familyBHead;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }
}
