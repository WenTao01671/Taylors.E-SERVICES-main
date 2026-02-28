package edu.taylors.io.capstone.eservices.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import edu.taylors.io.capstone.eservices.entity.TwoFactorMethod;
import edu.taylors.io.capstone.eservices.entity.User;
import edu.taylors.io.capstone.eservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    @Value("${app.name}")
    private String appName;

    @Value("${app.2fa.otp.expiration}")
    private Long otpExpiration;

    // ==========================================
    // EMAIL OTP Methods
    // ==========================================

    @Transactional
    public void sendEmailOtp(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate 6-digit OTP
        String otp = generateNumericOtp(6);

        // Save OTP to database
        user.setEmailOtp(otp);
        user.setEmailOtpExpiryDate(LocalDateTime.now().plusSeconds(otpExpiration / 1000));
        user.setEmailOtpVerified(false);
        userRepository.save(user);

        // Send email
        emailService.sendOtpEmail(user.getEmail(), otp, user.getStudentId());
    }

    public boolean verifyEmailOtp(String studentId, String otp) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if OTP exists and not expired
        if (user.getEmailOtp() == null || user.getEmailOtpExpiryDate() == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(user.getEmailOtpExpiryDate())) {
            return false; // OTP expired
        }

        return user.getEmailOtp().equals(otp);
    }

    @Transactional
    public void markEmailOtpAsVerified(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmailOtpVerified(true);
        user.setEmailOtp(null); // Clear OTP after verification
        userRepository.save(user);
    }

    // ==========================================
    // GOOGLE AUTHENTICATOR (TOTP) Methods
    // ==========================================

    @Transactional
    public String generateGoogleAuthSecret(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate secret key
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        // Save secret to database
        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        return secret;
    }

    public String generateQrCodeUrl(String studentId, String secret) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                appName,
                studentId,
                new GoogleAuthenticatorKey.Builder(secret).build()
        );
    }

    public String generateQrCodeBase64(String studentId, String secret) {
        try {
            String qrCodeUrl = generateQrCodeUrl(studentId, secret);

            BitMatrix matrix = new MultiFormatWriter().encode(
                    qrCodeUrl,
                    BarcodeFormat.QR_CODE,
                    300,
                    300
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);

            byte[] qrCodeBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(qrCodeBytes);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    public boolean verifyGoogleAuthCode(String studentId, int code) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTwoFactorSecret() == null) {
            return false;
        }

        return googleAuthenticator.authorize(user.getTwoFactorSecret(), code);
    }

    // ==========================================
    // Enable/Disable 2FA
    // ==========================================

    @Transactional
    public void enable2FA(String studentId, TwoFactorMethod method) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTwoFactorEnabled(true);
        user.setTwoFactorMethod(method);
        userRepository.save(user);

        // Send confirmation email
        emailService.send2FASetupEmail(user.getEmail(), user.getStudentId(), method.toString());
    }

    @Transactional
    public void disable2FA(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTwoFactorEnabled(false);
        user.setTwoFactorMethod(TwoFactorMethod.NONE);
        user.setTwoFactorSecret(null);
        user.setEmailOtp(null);
        user.setEmailOtpExpiryDate(null);
        userRepository.save(user);
    }

    public boolean is2FAEnabled(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.isTwoFactorEnabled();
    }

    public TwoFactorMethod get2FAMethod(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getTwoFactorMethod();
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private String generateNumericOtp(int length) {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
}