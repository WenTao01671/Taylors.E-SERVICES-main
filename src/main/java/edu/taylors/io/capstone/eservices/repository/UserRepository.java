package edu.taylors.io.capstone.eservices.repository;

import edu.taylors.io.capstone.eservices.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByEmail(String email);

    Optional<User> findByRefreshToken(String refreshToken);

    boolean existsByStudentId(String studentId);

    boolean existsByEmail(String email);
}