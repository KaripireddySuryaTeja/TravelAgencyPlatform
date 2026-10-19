package com.epam.edp.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Mail configuration class that explicitly configures JavaMailSender.
 * This ensures mail is properly configured even if .env loading has issues.
 */
@Configuration
@Slf4j
public class MailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    /**
     * Local dev only: no real SMTP server is configured, so instead of failing
     * (and breaking sign-up/password-reset flows), log the email body — the
     * verification/reset code can be read straight from the backend console.
     */
    @Bean
    @Profile("mock-mongo")
    public JavaMailSender loggingJavaMailSender() {
        return new JavaMailSenderImpl() {
            @Override
            public void send(SimpleMailMessage simpleMessage) {
                log.info("==================== MOCK EMAIL (mock-mongo profile) ====================");
                log.info("To: {}", (Object[]) simpleMessage.getTo());
                log.info("Subject: {}", simpleMessage.getSubject());
                log.info("Body:\n{}", simpleMessage.getText());
                log.info("===========================================================================");
            }

            @Override
            public void send(SimpleMailMessage... simpleMessages) {
                for (SimpleMailMessage message : simpleMessages) {
                    send(message);
                }
            }
        };
    }

    @Bean
    @Profile("!mock-mongo")
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.debug", "true"); // Enable debug mode to see SMTP communication

        // Log configuration (without password for security)
        log.info("Mail configuration initialized:");
        log.info("  Host: {}", mailHost);
        log.info("  Port: {}", mailPort);
        log.info("  Username: {}", mailUsername != null && !mailUsername.isEmpty() ? mailUsername : "NOT SET");
        log.info("  Password: {}", mailPassword != null && !mailPassword.isEmpty() ? "****" : "NOT SET");

        if (mailUsername == null || mailUsername.isEmpty()) {
            log.warn("MAIL_USERNAME is not set! Email sending will fail.");
        }
        if (mailPassword == null || mailPassword.isEmpty()) {
            log.warn("MAIL_PASSWORD is not set! Email sending will fail.");
        }

        return mailSender;
    }
}

