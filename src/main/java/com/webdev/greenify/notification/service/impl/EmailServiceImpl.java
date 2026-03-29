package com.webdev.greenify.notification.service.impl;

import com.webdev.greenify.common.exception.EmailSendingException;
import com.webdev.greenify.notification.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send email", e);
        }
    }

    @Override
    public void sendVerificationEmail(String to, String name, String token) {
        String subject = "Email Verification";
        String verificationUrl = "http://localhost:8080/api/v1/auth/verify?token=" + token;
        String content = "<p>Hi " + name + ",</p>" +
                "<p>Please click the link below to verify your registration:</p>" +
                "<p><a href=\"" + verificationUrl + "\">Verify Email</a></p>";
        sendEmail(to, subject, content);
    }
}
