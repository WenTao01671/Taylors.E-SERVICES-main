package edu.taylors.io.capstone.eservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String ipAddress;
    private String userAgent;
    private String location;

    @Column(nullable = false)
    private LocalDateTime loginTime;

    private boolean successful;

    @PrePersist
    protected void onCreate() {
        loginTime = LocalDateTime.now();
    }
}