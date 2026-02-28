package edu.taylors.io.capstone.eservices.repository;

import edu.taylors.io.capstone.eservices.entity.PasswordResetToken;
import edu.taylors.io.capstone.eservices.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUser(User user);

    // Clean up expired tokens
    void deleteByExpiryDateBefore(LocalDateTime date);

    // Delete all tokens for a user
    void deleteByUser(User user);
}