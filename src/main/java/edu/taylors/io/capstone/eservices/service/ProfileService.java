package edu.taylors.io.capstone.eservices.service;

import edu.taylors.io.capstone.eservices.dto.LoginHistoryDTO;
import edu.taylors.io.capstone.eservices.dto.ProfileDTO;
import edu.taylors.io.capstone.eservices.dto.UpdateProfileRequest;
import edu.taylors.io.capstone.eservices.entity.LoginHistory;
import edu.taylors.io.capstone.eservices.entity.User;
import edu.taylors.io.capstone.eservices.repository.LoginHistoryRepository;
import edu.taylors.io.capstone.eservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileDTO getProfile(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ProfileDTO.builder()
                .studentId(user.getStudentId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .profilePictureUrl(user.getProfilePictureUrl())
                .role(user.getRole())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .twoFactorMethod(user.getTwoFactorMethod() != null ?
                        user.getTwoFactorMethod().toString() : "NONE")
                .build();
    }

    @Transactional
    public ProfileDTO updateProfile(String studentId, UpdateProfileRequest request) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update fields if provided
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if new email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        userRepository.save(user);

        return getProfile(studentId);
    }

    @Transactional
    public void changePassword(String studentId, String currentPassword,
                               String newPassword, String confirmPassword) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Verify new password matches confirmation
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("New passwords do not match");
        }

        // Validate new password strength (at least 6 characters)
        if (newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters long");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateProfilePicture(String studentId, String pictureUrl) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setProfilePictureUrl(pictureUrl);
        userRepository.save(user);
    }

    public List<LoginHistoryDTO> getLoginHistory(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<LoginHistory> history = loginHistoryRepository
                .findTop10ByUserOrderByLoginTimeDesc(user);

        return history.stream()
                .map(h -> LoginHistoryDTO.builder()
                        .id(h.getId())
                        .ipAddress(h.getIpAddress())
                        .userAgent(h.getUserAgent())
                        .location(h.getLocation())
                        .loginTime(h.getLoginTime())
                        .successful(h.isSuccessful())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void recordLogin(String studentId, String ipAddress,
                            String userAgent, boolean successful) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LoginHistory loginHistory = LoginHistory.builder()
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .location("Unknown") // You can integrate IP geolocation API here
                .successful(successful)
                .build();

        loginHistoryRepository.save(loginHistory);

        // Update last login time
        if (successful) {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
}