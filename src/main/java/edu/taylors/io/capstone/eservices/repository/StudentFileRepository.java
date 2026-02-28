package edu.taylors.io.capstone.eservices.repository;

import edu.taylors.io.capstone.eservices.entity.FileCategory;
import edu.taylors.io.capstone.eservices.entity.StudentFile;
import edu.taylors.io.capstone.eservices.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentFileRepository extends JpaRepository<StudentFile, Long> {

    List<StudentFile> findByUserOrderByUploadedAtDesc(User user);

    List<StudentFile> findByUserAndCategoryOrderByUploadedAtDesc(User user, FileCategory category);

    Optional<StudentFile> findByIdAndUser(Long id, User user);

    long countByUser(User user);

    // ADD @Query annotation for custom sum query
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM StudentFile f WHERE f.user = :user")
    Long sumFileSizeByUser(@Param("user") User user);
}