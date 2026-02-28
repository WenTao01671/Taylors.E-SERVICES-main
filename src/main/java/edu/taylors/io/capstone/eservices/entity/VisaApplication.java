package edu.taylors.io.capstone.eservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Visa Application entity - tracks student visa processing
 */
@Entity
@Table(name = "visa_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisaApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User student;

    // Application Reference
    @Column(unique = true)
    private String applicationNumber;

    // EMGS References
    private String emgsReference;
    private String valNumber;  // Visa Approval Letter number

    // Visa Type
    private String visaType;  // STUDENT_PASS, ENTRY_VISA

    // Current Status
    @Column(nullable = false)
    private String status;  // PENDING, DOCUMENTS_SUBMITTED, EMGS_PROCESSING, VAL_ISSUED, etc.

    // Progress
    private Integer progressPercentage;
    private String currentStage;  // Human-readable stage

    // Important Dates
    private LocalDateTime applicationDate;
    private LocalDateTime documentsSubmittedDate;
    private LocalDateTime emgsSubmissionDate;
    private LocalDateTime emgsApprovalDate;
    private LocalDateTime valIssuedDate;
    private LocalDateTime valExpiryDate;
    private LocalDateTime immigrationSubmissionDate;
    private LocalDateTime immigrationApprovalDate;
    private LocalDateTime passCollectedDate;

    // Passport Information
    private String passportNumber;
    private LocalDateTime passportExpiry;
    private String nationality;

    // Program Information
    private String programName;
    private String faculty;
    private LocalDateTime programStartDate;

    // Processing Details
    private String processingNotes;
    private String rejectionReason;

    // Link to Medical Examination
    @OneToOne
    @JoinColumn(name = "medical_examination_id")
    private MedicalExamination medicalExamination;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastUpdatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (progressPercentage == null) {
            progressPercentage = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to calculate progress
    public void updateProgress() {
        int progress = 0;

        // Application created: 5%
        if (applicationDate != null) progress += 5;

        // Documents submitted: 15%
        if (documentsSubmittedDate != null) progress += 15;

        // Medical cleared: 10%
        if (medicalExamination != null &&
                medicalExamination.getStatus().equals("PASSED")) {
            progress += 10;
        }

        // EMGS submission: 10%
        if (emgsSubmissionDate != null) progress += 10;

        // EMGS approval (32%): 20%
        if (emgsApprovalDate != null) progress += 20;

        // VAL issued (70%): 20%
        if (valIssuedDate != null) progress += 20;

        // Immigration submission: 10%
        if (immigrationSubmissionDate != null) progress += 10;

        // Immigration approval: 5%
        if (immigrationApprovalDate != null) progress += 5;

        // Pass collected: 5%
        if (passCollectedDate != null) progress += 5;

        this.progressPercentage = progress;
    }

    // Check if medical is required and completed
    public boolean isMedicalComplete() {
        return medicalExamination != null &&
                medicalExamination.getStatus().equals("PASSED");
    }

    // Check if ready for EMGS submission
    public boolean isReadyForEmgsSubmission() {
        return documentsSubmittedDate != null && isMedicalComplete();
    }
}
