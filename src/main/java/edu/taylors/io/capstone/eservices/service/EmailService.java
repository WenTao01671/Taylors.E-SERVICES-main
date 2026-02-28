package edu.taylors.io.capstone.eservices.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name}")
    private String appName;

    public void sendEmail(String toEmail, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    public void sendOtpEmail(String toEmail, String otp, String studentId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(appName + " - Your OTP Code");
        message.setText(
                "Hello " + studentId + ",\n\n" +
                        "Your OTP code is: " + otp + "\n\n" +
                        "This code will expire in 5 minutes.\n\n" +
                        "If you didn't request this code, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        appName + " Team"
        );

        mailSender.send(message);
    }

    public void send2FASetupEmail(String toEmail, String studentId, String method) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(appName + " - 2FA Enabled");
        message.setText(
                "Hello " + studentId + ",\n\n" +
                        "Two-Factor Authentication has been successfully enabled on your account.\n\n" +
                        "Method: " + method + "\n\n" +
                        "If you didn't enable this, please contact support immediately.\n\n" +
                        "Best regards,\n" +
                        appName + " Team"
        );

        mailSender.send(message);
    }
    public void sendPasswordResetEmail(String toEmail, String resetToken, String studentId) {
        String resetLink = "http://localhost:9090/api/auth/reset-password?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(appName + " - Password Reset Request");
        message.setText(
                "Hello " + studentId + ",\n\n" +
                        "You requested to reset your password.\n\n" +
                        "Please click the link below to reset your password:\n" +
                        resetLink + "\n\n" +
                        "Or use this token: " + resetToken + "\n\n" +
                        "This link will expire in 1 hour.\n\n" +
                        "If you didn't request this, please ignore this email and your password will remain unchanged.\n\n" +
                        "Best regards,\n" +
                        appName + " Team"
        );

        mailSender.send(message);
    }

    public void sendPasswordChangedConfirmation(String toEmail, String studentId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(appName + " - Password Changed Successfully");
        message.setText(
                "Hello " + studentId + ",\n\n" +
                        "Your password has been changed successfully.\n\n" +
                        "If you didn't make this change, please contact support immediately.\n\n" +
                        "Best regards,\n" +
                        appName + " Team"
        );

        mailSender.send(message);
    }
}