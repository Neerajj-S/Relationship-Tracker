package org.relativesdb.connection.repository;

import org.relativesdb.connection.model.Relations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RelationsRepository extends JpaRepository<Relations, Long> {
    
    @Query("SELECT r FROM Relations r WHERE r.familyAHead.nationalId = :nid OR r.familyBHead.nationalId = :nid")
    List<Relations> findAlliancesForFamily(@Param("nid") String nationalId);
}