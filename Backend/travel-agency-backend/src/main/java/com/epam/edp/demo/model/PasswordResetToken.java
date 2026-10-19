package com.epam.edp.demo.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    private String id;

    @Indexed
    private String email;

    private String code;

    private boolean used;

    @CreatedDate
    private Instant createdAt;

    @Indexed(expireAfterSeconds = 900) // 15 minutes TTL
    private Instant expiresAt;

    public PasswordResetToken(String email, String code) {
        this.email = email;
        this.code = code;
        this.used = false;
        this.expiresAt = Instant.now().plusSeconds(900); // 15 minutes
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
