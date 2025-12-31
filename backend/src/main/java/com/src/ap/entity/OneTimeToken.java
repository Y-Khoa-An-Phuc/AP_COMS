package com.src.ap.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a one-time use token for various user actions.
 * Tokens can be used for first-time login, password reset, etc.
 */
@Entity
@Table(name = "one_time_tokens", indexes = {
        @Index(name = "idx_token", columnList = "token"),
        @Index(name = "idx_user_type", columnList = "user_id, type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneTimeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private TokenType type;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
