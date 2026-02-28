package edu.taylors.io.capstone.eservices.repository;

import edu.taylors.io.capstone.eservices.entity.VisaApplication;
import edu.taylors.io.capstone.eservices.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisaApplicationRepository extends JpaRepository<VisaApplication, Long> {

    Optional<VisaApplication> findByStudent(User student);

    Optional<VisaApplication> findByStudentStudentId(String studentId);

    Optional<VisaApplication> findByApplicationNumber(String applicationNumber);

    List<VisaApplication> findByStatus(String status);

    @Query("SELECT v FROM VisaApplication v WHERE v.valExpiryDate < :now AND v.status = 'VAL_ISSUED'")
    List<VisaApplication> findExpiredVALs(@Param("now") LocalDateTime now);

    @Query("SELECT v FROM VisaApplication v WHERE v.status = 'EMGS_APPROVED' AND v.valIssuedDate IS NULL")
    List<VisaApplication> findPendingVALIssuance();

    Long countByStatus(String status);
}
