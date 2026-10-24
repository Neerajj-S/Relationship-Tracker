package org.relativesdb.connection.model;

public class MemberKinshipDTO {
    private String personA;
    private String personAHead;
    private String relationAtoB;
    private String relationBtoA;
    private String personB;
    private String personBHead;

    public MemberKinshipDTO(String personA, String personAHead, String relationAtoB, String relationBtoA, String personB, String personBHead) {
        this.personA = personA;
        this.personAHead = personAHead;
        this.relationAtoB = relationAtoB;
        this.relationBtoA = relationBtoA;
        this.personB = personB;
        this.personBHead = personBHead;
    }

    public String getPersonA() { return personA; }
    public String getPersonAHead() { return personAHead; }
    public String getRelationAtoB() { return relationAtoB; }
    public String getRelationBtoA() { return relationBtoA; }
    public String getPersonB() { return personB; }
    public String getPersonBHead() { return personBHead; }
}