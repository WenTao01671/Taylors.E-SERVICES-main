package edu.taylors.io.capstone.eservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Medical Examination entity - tracks student medical check-ups for visa processing
 */
@Entity
@Table(name = "medical_examinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalExamination {

    @OneToOne(mappedBy = "medicalExamination")
    private VisaApplication visaApplication;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User student;

    // Reference Number
    @Column(unique = true)
    private String examinationNumber;

    // Appointment Details
    private LocalDateTime appointmentDate;
    private String clinicName;
    private String clinicAddress;
    private String clinicPhone;

    // Status - Using String instead of enum for simplicity
    @Column(nullable = false)
    private String status; // PENDING, SCHEDULED, COMPLETED, PASSED, FAILED, SUBMITTED_TO_EMGS

    // Examination Results
    private LocalDateTime examinationDate;
    private LocalDateTime resultDate;
    private Boolean passed;
    private String resultNotes;

    // Required Tests
    private Boolean chestXrayDone;
    private Boolean bloodTestDone;
    private Boolean urineTestDone;

    // Documents
    @ManyToOne
    @JoinColumn(name = "medical_report_file_id")
    private StudentFile medicalReportFile;

    // EMGS Submission
    private Boolean submittedToEmgs;
    private LocalDateTime emgsSubmissionDate;
    private String emgsReference;

    // Validity
    private LocalDateTime expiryDate; // Valid for 3 months

    // Progress
    private Integer progressPercentage;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    // Helper method to check if all tests are done
    public boolean allTestsCompleted() {
        return Boolean.TRUE.equals(chestXrayDone) &&
                Boolean.TRUE.equals(bloodTestDone) &&
                Boolean.TRUE.equals(urineTestDone);
    }

    // Calculate progress
    public void updateProgress() {
        int progress = 0;

        // Appointment scheduled: 20%
        if (appointmentDate != null) progress += 20;

        // Examination done: 20%
        if (examinationDate != null) progress += 20;

        // Tests completed: 40%
        if (chestXrayDone != null && chestXrayDone) progress += 13;
        if (bloodTestDone != null && bloodTestDone) progress += 13;
        if (urineTestDone != null && urineTestDone) progress += 14;

        // Results: 10%
        if (resultDate != null) progress += 10;

        // EMGS submission: 10%
        if (submittedToEmgs != null && submittedToEmgs) progress += 10;

        this.progressPercentage = progress;
    }
}
