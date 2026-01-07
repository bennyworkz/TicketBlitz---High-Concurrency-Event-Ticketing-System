package com.ticketblitz.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * User Entity
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;  // BCrypt hashed

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role = "USER";  // USER, ADMIN

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
