package be.delomid.oneapp.mschat.mschat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    public void sendOtpEmail(String to, String otpCode, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Code de vérification - MSChat");
            message.setText(buildOtpEmailContent(otpCode, purpose));
            
            mailSender.send(message);
            log.debug("OTP email sent to: {}", to);
            
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", to, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
    
    public void sendAccountStatusEmail(String to, String status, String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Statut de votre compte - MSChat");
            message.setText(buildAccountStatusEmailContent(status, reason));
            
            mailSender.send(message);
            log.debug("Account status email sent to: {}", to);
            
        } catch (Exception e) {
            log.error("Failed to send account status email to: {}", to, e);
        }
    }
    
    private String buildOtpEmailContent(String otpCode, String purpose) {
        return String.format("""
            Bonjour,
            
            Votre code de vérification pour %s est : %s
            
            Ce code expire dans 10 minutes.
            
            Si vous n'avez pas demandé ce code, veuillez ignorer cet email.
            
            Cordialement,
            L'équipe MSChat
            """, purpose, otpCode);
    }
    
    private String buildAccountStatusEmailContent(String status, String reason) {
        String statusText = switch (status) {
            case "ACTIVE" -> "approuvé";
            case "BLOCKED" -> "bloqué";
            case "REJECTED" -> "rejeté";
            default -> status.toLowerCase();
        };
        
        return String.format("""
            Bonjour,
            
            Le statut de votre compte MSChat a été mis à jour : %s
            
            %s
            
            Cordialement,
            L'équipe MSChat
            """, statusText, reason != null ? "Raison : " + reason : "");
    }
}