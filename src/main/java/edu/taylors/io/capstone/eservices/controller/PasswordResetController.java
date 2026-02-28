package edu.taylors.io.capstone.eservices.controller;

import edu.taylors.io.capstone.eservices.dto.ForgotPasswordRequest;
import edu.taylors.io.capstone.eservices.dto.ResetPasswordRequest;
import edu.taylors.io.capstone.eservices.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Step 1: Request password reset
     * User provides email or student ID
     * System sends reset link via email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.initiatePasswordReset(request.getEmailOrStudentId());

            return ResponseEntity.ok(Map.of(
                    "message", "If an account exists with this information, a password reset link has been sent to the registered email.",
                    "note", "Please check your email inbox and spam folder."
            ));
        } catch (Exception e) {
            // Don't reveal if user exists or not (security best practice)
            return ResponseEntity.ok(Map.of(
                    "message", "If an account exists with this information, a password reset link has been sent to the registered email.",
                    "note", "Please check your email inbox and spam folder."
            ));
        }
    }

    /**
     * Step 2: Validate reset token
     * Check if token is valid and not expired
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        boolean isValid = passwordResetService.validateResetToken(token);

        if (isValid) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "message", "Token is valid"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", "Invalid or expired token"
            ));
        }
    }

    /**
     * Step 3: Reset password with token
     * User provides token and new password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(
                    request.getToken(),
                    request.getNewPassword(),
                    request.getConfirmPassword()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Password has been reset successfully",
                    "note", "You can now login with your new password"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to reset password. Please try again."
            ));
        }
    }
}