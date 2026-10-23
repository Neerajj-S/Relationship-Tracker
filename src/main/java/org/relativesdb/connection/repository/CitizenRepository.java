package org.relativesdb.connection.repository;

import org.relativesdb.connection.model.Citizen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CitizenRepository extends JpaRepository<Citizen, Long> {
    
    Citizen findByNationalId(String nationalId);
    
    List<Citizen> findByFamilyHeadNationalId(String headNationalId);
    
    List<Citizen> findByFullNameContainingIgnoreCase(String keyword);
}