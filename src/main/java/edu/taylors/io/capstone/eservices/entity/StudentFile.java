package edu.taylors.io.capstone.eservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String fileType; // MIME type (e.g., application/pdf, image/jpeg)

    @Column(nullable = false)
    private Long fileSize; // in bytes

    @Column(nullable = false)
    private String filePath; // where the file is stored

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileCategory category;

    private String description;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to format file size
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }
}