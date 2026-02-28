package edu.taylors.io.capstone.eservices.service;

import edu.taylors.io.capstone.eservices.entity.User;
import edu.taylors.io.capstone.eservices.repository.UserRepository;
import edu.taylors.io.capstone.eservices.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public String createRefreshToken(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate new refresh token
        String refreshToken = jwtUtil.generateRefreshToken();

        // Convert Date to LocalDateTime
        LocalDateTime expiryDate = LocalDateTime.ofInstant(
                jwtUtil.getRefreshTokenExpiryDate().toInstant(),
                ZoneId.systemDefault()
        );

        // Save to database
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiryDate(expiryDate);
        userRepository.save(user);

        return refreshToken;
    }

    public Optional<User> findByRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken);
    }

    public boolean validateRefreshToken(String refreshToken) {
        Optional<User> userOpt = findByRefreshToken(refreshToken);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Check if token is expired
        return user.getRefreshTokenExpiryDate() != null
                && user.getRefreshTokenExpiryDate().isAfter(LocalDateTime.now());
    }

    @Transactional
    public void deleteRefreshToken(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRefreshToken(null);
        user.setRefreshTokenExpiryDate(null);
        userRepository.save(user);
    }
}