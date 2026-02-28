package edu.taylors.io.capstone.eservices.controller;

import edu.taylors.io.capstone.eservices.dto.ChangePasswordRequest;
import edu.taylors.io.capstone.eservices.dto.LoginHistoryDTO;
import edu.taylors.io.capstone.eservices.dto.ProfileDTO;
import edu.taylors.io.capstone.eservices.dto.UpdateProfileRequest;
import edu.taylors.io.capstone.eservices.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileDTO> getProfile(Authentication authentication) {
        String studentId = authentication.getName();
        ProfileDTO profile = profileService.getProfile(studentId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {
        try {
            String studentId = authentication.getName();
            ProfileDTO updatedProfile = profileService.updateProfile(studentId, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully",
                    "profile", updatedProfile
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request) {
        try {
            String studentId = authentication.getName();
            profileService.changePassword(
                    studentId,
                    request.getCurrentPassword(),
                    request.getNewPassword(),
                    request.getConfirmPassword()
            );
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/picture")
    public ResponseEntity<?> uploadProfilePicture(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        try {
            // For now, we'll just save the filename
            // In production, you'd upload to cloud storage (AWS S3, Azure Blob, etc.)
            String studentId = authentication.getName();
            String pictureUrl = "/uploads/" + file.getOriginalFilename();

            profileService.updateProfilePicture(studentId, pictureUrl);

            return ResponseEntity.ok(Map.of(
                    "message", "Profile picture uploaded successfully",
                    "url", pictureUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to upload picture: " + e.getMessage()));
        }
    }

    @GetMapping("/login-history")
    public ResponseEntity<List<LoginHistoryDTO>> getLoginHistory(Authentication authentication) {
        String studentId = authentication.getName();
        List<LoginHistoryDTO> history = profileService.getLoginHistory(studentId);
        return ResponseEntity.ok(history);
    }
}