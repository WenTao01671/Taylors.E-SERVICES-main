package edu.taylors.io.capstone.eservices.controller;

import edu.taylors.io.capstone.eservices.entity.TwoFactorMethod;
import edu.taylors.io.capstone.eservices.entity.User;
import edu.taylors.io.capstone.eservices.repository.UserRepository;
import edu.taylors.io.capstone.eservices.security.JwtUtil;
import edu.taylors.io.capstone.eservices.service.ProfileService;
import edu.taylors.io.capstone.eservices.service.RefreshTokenService;
import edu.taylors.io.capstone.eservices.service.TwoFactorAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final ProfileService profileService;


    /**
     * LOGIN ENDPOINT - ALWAYS REQUIRES OTP
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request) {
        try {
            System.out.println("=== LOGIN ATTEMPT ===");
            System.out.println("Student ID: " + loginRequest.getStudentId());

            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getStudentId(),
                            loginRequest.getPassword()
                    )
            );

            // Get user info
            User user = userRepository.findByStudentId(loginRequest.getStudentId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Record successful login attempt
            try {
                String ipAddress = request.getRemoteAddr();
                String userAgent = request.getHeader("User-Agent");
                profileService.recordLogin(loginRequest.getStudentId(), ipAddress, userAgent, true);
            } catch (Exception e) {
                System.err.println("Failed to record login: " + e.getMessage());
            }

            // ALWAYS send OTP (mandatory for all logins)
            twoFactorAuthService.sendEmailOtp(loginRequest.getStudentId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "OTP sent to your email");
            response.put("requiresOTP", true);
            response.put("studentId", user.getStudentId());
            response.put("email", maskEmail(user.getEmail()));
            response.put("nextStep", "Please enter the 6-digit OTP code sent to your email");

            System.out.println("OTP sent to: " + user.getEmail());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            System.err.println("Bad credentials for: " + loginRequest.getStudentId());

            // Record failed login attempt
            try {
                String ipAddress = request.getRemoteAddr();
                String userAgent = request.getHeader("User-Agent");
                profileService.recordLogin(loginRequest.getStudentId(), ipAddress, userAgent, false);
            } catch (Exception ex) {
                System.err.println("Failed to record failed login: " + ex.getMessage());
            }

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid student ID or password");
            return ResponseEntity.status(401).body(errorResponse);

        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Login failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * VERIFY LOGIN OTP - Required for all logins
     */
    @PostMapping("/login/verify-otp")
    public ResponseEntity<?> verifyLoginOtp(@RequestBody OtpVerificationRequest request) {
        try {
            User user = userRepository.findByStudentId(request.getStudentId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify OTP
            boolean verified = twoFactorAuthService.verifyEmailOtp(request.getStudentId(), request.getOtp());

            if (!verified) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid or expired OTP code");
                return ResponseEntity.status(401).body(errorResponse);
            }

            // Mark OTP as verified
            twoFactorAuthService.markEmailOtpAsVerified(request.getStudentId());

            // OTP verified - generate tokens
            final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getStudentId());
            final String accessToken = jwtUtil.generateAccessToken(userDetails);
            final String refreshToken = refreshTokenService.createRefreshToken(request.getStudentId());

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 900);
            response.put("studentId", user.getStudentId());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("message", "Login successful");

            System.out.println("Login successful with OTP for: " + user.getStudentId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "OTP verification failed: " + e.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    /**
     * OLD ENDPOINT - Still supported for backward compatibility
     */
    @PostMapping("/login/verify-2fa")
    public ResponseEntity<?> verifyLoginWith2FA(@RequestBody Verify2FALoginRequest request) {
        try {
            User user = userRepository.findByStudentId(request.getStudentId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean verified = false;

            if (user.getTwoFactorMethod() == TwoFactorMethod.EMAIL_OTP) {
                verified = twoFactorAuthService.verifyEmailOtp(request.getStudentId(), request.getOtp());
                if (verified) {
                    twoFactorAuthService.markEmailOtpAsVerified(request.getStudentId());
                }
            } else if (user.getTwoFactorMethod() == TwoFactorMethod.GOOGLE_AUTHENTICATOR) {
                verified = twoFactorAuthService.verifyGoogleAuthCode(request.getStudentId(), Integer.parseInt(request.getOtp()));
            }

            if (!verified) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid verification code");
                return ResponseEntity.status(401).body(errorResponse);
            }

            // 2FA verified - generate tokens
            final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getStudentId());
            final String accessToken = jwtUtil.generateAccessToken(userDetails);
            final String refreshToken = refreshTokenService.createRefreshToken(request.getStudentId());

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 900);
            response.put("studentId", user.getStudentId());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("message", "Login successful with 2FA");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "2FA verification failed");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    /**
     * REFRESH TOKEN ENDPOINT
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!refreshTokenService.validateRefreshToken(refreshToken)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid or expired refresh token");
            return ResponseEntity.status(401).body(errorResponse);
        }

        // Get user from refresh token
        User user = refreshTokenService.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getStudentId());

        // Generate new ACCESS token
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 900);
        response.put("message", "Token refreshed successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * LOGOUT ENDPOINT
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        try {
            refreshTokenService.deleteRefreshToken(request.getStudentId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Logout failed");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * REGISTER ENDPOINT - Automatically enables OTP and sends verification email
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Validate student ID doesn't exist
            if (userRepository.existsByStudentId(request.getStudentId())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Student ID already exists");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validate email doesn't exist
            if (userRepository.existsByEmail(request.getEmail())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Email already exists");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Create new user with 2FA ENABLED by default
            User user = User.builder()
                    .studentId(request.getStudentId())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(request.getRole() != null ? request.getRole() : "STUDENT")
                    .enabled(true)
                    .twoFactorEnabled(true)  // ← ALWAYS ENABLED
                    .twoFactorMethod(TwoFactorMethod.EMAIL_OTP)  // ← DEFAULT TO EMAIL
                    .build();

            userRepository.save(user);

            // Send verification OTP to email
            try {
                twoFactorAuthService.sendEmailOtp(user.getStudentId());
            } catch (Exception e) {
                System.err.println("Failed to send OTP: " + e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Registration successful! OTP sent to your email for verification.");
            response.put("studentId", user.getStudentId());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("requiresVerification", true);
            response.put("nextStep", "Please verify your email with the OTP code sent to complete registration.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * VERIFY REGISTRATION OTP
     */
    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> verifyRegistrationOtp(@RequestBody OtpVerificationRequest request) {
        try {
            User user = userRepository.findByStudentId(request.getStudentId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify OTP
            boolean verified = twoFactorAuthService.verifyEmailOtp(request.getStudentId(), request.getOtp());

            if (!verified) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid or expired OTP code");
                return ResponseEntity.status(401).body(errorResponse);
            }

            // Mark OTP as verified
            twoFactorAuthService.markEmailOtpAsVerified(request.getStudentId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Email verified successfully! You can now login.");
            response.put("studentId", user.getStudentId());
            response.put("verified", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Verification failed: " + e.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    /**
     * RESEND OTP - For both registration and login
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequest request) {
        try {
            User user = userRepository.findByStudentId(request.getStudentId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Send new OTP
            twoFactorAuthService.sendEmailOtp(request.getStudentId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "New OTP sent to your email");
            response.put("email", maskEmail(user.getEmail()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to resend OTP: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * TEST ENDPOINT
     */
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Auth endpoint is working");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("otpMandatory", true);
        return ResponseEntity.ok(response);
    }

    /**
     * HELPER METHOD - Mask email for security
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return "**@" + domain;
        }

        return username.substring(0, 2) + "***@" + domain;
    }
}

// ==================== REQUEST DTO CLASSES ====================

class LoginRequest {
    private String studentId;
    private String password;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class RefreshTokenRequest {
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}

class LogoutRequest {
    private String studentId;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
}

class RegisterRequest {
    private String studentId;
    private String email;
    private String password;
    private String role;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}

class Verify2FALoginRequest {
    private String studentId;
    private String otp;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}

class OtpVerificationRequest {
    private String studentId;
    private String otp;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}

class ResendOtpRequest {
    private String studentId;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
}