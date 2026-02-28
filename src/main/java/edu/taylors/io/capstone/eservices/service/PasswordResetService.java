package edu.taylors.io.capstone.eservices.service;

import edu.taylors.io.capstone.eservices.entity.PasswordResetToken;
import edu.taylors.io.capstone.eservices.entity.User;
import edu.taylors.io.capstone.eservices.repository.PasswordResetTokenRepository;
import edu.taylors.io.capstone.eservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.token.expiration:3600000}") // 1 hour in milliseconds
    private Long tokenExpiration;

    @Transactional
    public void initiatePasswordReset(String emailOrStudentId) {
        // Find user by email or student ID
        User user = userRepository.findByEmail(emailOrStudentId)
                .or(() -> userRepository.findByStudentId(emailOrStudentId))
                .orElseThrow(() -> new RuntimeException("User not found with provided email or student ID"));

        // Delete any existing tokens for this user
        resetTokenRepository.deleteByUser(user);

        // Generate reset token
        String token = UUID.randomUUID().toString();

        // Calculate expiry date
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(tokenExpiration / 1000);

        // Save token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        resetTokenRepository.save(resetToken);

        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), token, user.getStudentId());
    }

    public boolean validateResetToken(String token) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElse(null);

        if (resetToken == null) {
            return false;
        }

        if (resetToken.isUsed()) {
            return false;
        }

        if (resetToken.isExpired()) {
            return false;
        }

        return true;
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Passwords do not match");
        }

        // Validate password strength
        if (newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters long");
        }

        // Find token
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        // Validate token
        if (resetToken.isUsed()) {
            throw new RuntimeException("This reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new RuntimeException("This reset token has expired");
        }

        // Get user
        User user = resetToken.getUser();

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        // Send confirmation email
        emailService.sendPasswordChangedConfirmation(user.getEmail(), user.getStudentId());
    }

    @Transactional
    public void cleanupExpiredTokens() {
        resetTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}