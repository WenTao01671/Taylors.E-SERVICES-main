package edu.taylors.io.capstone.eservices.controller;

import edu.taylors.io.capstone.eservices.entity.TwoFactorMethod;
import edu.taylors.io.capstone.eservices.service.TwoFactorAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/2fa")
@RequiredArgsConstructor
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;


    // EMAIL OTP Endpoints
    @PostMapping("/email/setup")
    public ResponseEntity<?> setupEmailOtp(Authentication authentication) {
        String studentId = authentication.getName();

        try {
            twoFactorAuthService.sendEmailOtp(studentId);
            twoFactorAuthService.enable2FA(studentId, TwoFactorMethod.EMAIL_OTP);

            return ResponseEntity.ok(Map.of(
                    "message", "OTP sent to your email. Please verify to complete setup.",
                    "method", "EMAIL_OTP"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to setup Email OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/email/send-otp")
    public ResponseEntity<?> sendEmailOtp(Authentication authentication) {
        String studentId = authentication.getName();

        try {
            twoFactorAuthService.sendEmailOtp(studentId);
            return ResponseEntity.ok(Map.of("message", "OTP sent to your email"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to send OTP: " + e.getMessage()));
        }
    }

    //Headers: Authorization: Bearer <access_token>
    //Body: { "otp": "123456" }
    @PostMapping("/email/verify")
    public ResponseEntity<?> verifyEmailOtp(
            Authentication authentication,
            @RequestBody VerifyOtpRequest request) {

        String studentId = authentication.getName();

        boolean isValid = twoFactorAuthService.verifyEmailOtp(studentId, request.getOtp());

        if (isValid) {
            twoFactorAuthService.markEmailOtpAsVerified(studentId);
            return ResponseEntity.ok(Map.of(
                    "message", "OTP verified successfully",
                    "verified", true
            ));
        } else {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "error", "Invalid or expired OTP",
                            "verified", false
                    ));
        }
    }

    // GOOGLE AUTHENTICATOR Endpoints

    @PostMapping("/google-auth/setup")
    public ResponseEntity<?> setupGoogleAuth(Authentication authentication) {
        String studentId = authentication.getName();

        try {
            // Generate secret
            String secret = twoFactorAuthService.generateGoogleAuthSecret(studentId);

            // Generate QR code
            String qrCodeUrl = twoFactorAuthService.generateQrCodeUrl(studentId, secret);
            String qrCodeBase64 = twoFactorAuthService.generateQrCodeBase64(studentId, secret);

            return ResponseEntity.ok(Map.of(
                    "message", "Scan this QR code with Google Authenticator app",
                    "secret", secret,
                    "qrCodeUrl", qrCodeUrl,
                    "qrCodeImage", "data:image/png;base64," + qrCodeBase64,
                    "instructions", "1. Open Google Authenticator app\n" +
                            "2. Tap '+' to add account\n" +
                            "3. Scan QR code or enter secret manually\n" +
                            "4. Verify with 6-digit code"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to setup Google Authenticator: " + e.getMessage()));
        }
    }


    //Headers: Authorization: Bearer <access_token>
    //Body: { "code": 123456 }

    @PostMapping("/google-auth/verify")
    public ResponseEntity<?> verifyGoogleAuth(
            Authentication authentication,
            @RequestBody VerifyGoogleAuthRequest request) {

        String studentId = authentication.getName();

        boolean isValid = twoFactorAuthService.verifyGoogleAuthCode(studentId, request.getCode());

        if (isValid) {
            twoFactorAuthService.enable2FA(studentId, TwoFactorMethod.GOOGLE_AUTHENTICATOR);
            return ResponseEntity.ok(Map.of(
                    "message", "Google Authenticator verified and enabled successfully",
                    "verified", true
            ));
        } else {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "error", "Invalid verification code",
                            "verified", false
                    ));
        }
    }


    // General 2FA Management


// To check if 2FA enabled
    @GetMapping("/status")
    public ResponseEntity<?> get2FAStatus(Authentication authentication) {
        String studentId = authentication.getName();

        boolean enabled = twoFactorAuthService.is2FAEnabled(studentId);
        TwoFactorMethod method = twoFactorAuthService.get2FAMethod(studentId);

        return ResponseEntity.ok(Map.of(
                "enabled", enabled,
                "method", method.toString()
        ));
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disable2FA(Authentication authentication) {
        String studentId = authentication.getName();

        try {
            twoFactorAuthService.disable2FA(studentId);
            return ResponseEntity.ok(Map.of("message", "2FA disabled successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to disable 2FA: " + e.getMessage()));
        }
    }
}

// Request DTOs
class VerifyOtpRequest {
    private String otp;

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}

class VerifyGoogleAuthRequest {
    private int code;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}