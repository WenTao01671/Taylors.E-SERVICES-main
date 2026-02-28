package edu.taylors.io.capstone.eservices.repository;

import edu.taylors.io.capstone.eservices.entity.MedicalExamination;
import edu.taylors.io.capstone.eservices.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalExaminationRepository extends JpaRepository<MedicalExamination, Long> {

    Optional<MedicalExamination> findByStudent(User student);

    Optional<MedicalExamination> findByStudentStudentId(String studentId);

    List<MedicalExamination> findByStatus(String status);

    @Query("SELECT m FROM MedicalExamination m WHERE m.expiryDate < :now AND m.status = 'PASSED'")
    List<MedicalExamination> findExpired(LocalDateTime now);
}
