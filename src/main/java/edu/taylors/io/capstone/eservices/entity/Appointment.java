package edu.taylors.io.capstone.eservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Appointment entity - for medical clinics AND International Office
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User student;

    @ManyToOne
    @JoinColumn(name = "medical_examination_id")
    private MedicalExamination medicalExamination;

    @ManyToOne
    @JoinColumn(name = "visa_application_id")
    private VisaApplication visaApplication;

    // Appointment Reference
    @Column(unique = true)
    private String appointmentNumber;  // APT-2026-00001

    // Appointment Type
    @Column(nullable = false)
    private String appointmentType;  // MEDICAL, OFFICE_CONSULTATION, DOCUMENT_SUBMISSION, VISA_INTERVIEW

    // Location Details
    private String locationName;     // "Pantai Hospital KL" or "International Office"
    private String locationAddress;
    private String locationPhone;
    private String locationEmail;
    private String roomNumber;       // For office appointments: "Room 301, Block A"

    // Appointment Date & Time
    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime appointmentTime;

    private Integer durationMinutes;  // Default: 30 min (office) or 60 min (medical)

    // Staff Assignment (for office appointments)
    @ManyToOne
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    // Status
    @Column(nullable = false)
    private String status;  // PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW, RESCHEDULED

    // Purpose/Reason
    private String purpose;  // "Visa document review", "Medical check-up", etc.
    private String description;

    // Confirmation
    private Boolean confirmedByStudent;
    private LocalDateTime confirmationDate;

    // Reminder
    private Boolean reminderSent;
    private LocalDateTime reminderSentDate;
    private Integer reminderCount;

    // Notes
    private String studentNotes;  // Student's special requests
    private String staffNotes;    // Staff/clinic notes
    private String cancellationReason;

    // Reschedule tracking
    private LocalDateTime originalDate;
    private Integer rescheduleCount;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (durationMinutes == null) {
            durationMinutes = appointmentType != null && appointmentType.equals("MEDICAL") ? 60 : 30;
        }
        if (rescheduleCount == null) {
            rescheduleCount = 0;
        }
        if (reminderCount == null) {
            reminderCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public LocalDateTime getAppointmentDateTime() {
        return LocalDateTime.of(appointmentDate, appointmentTime);
    }

    public boolean isPast() {
        return getAppointmentDateTime().isBefore(LocalDateTime.now());
    }

    public boolean isUpcoming() {
        return getAppointmentDateTime().isAfter(LocalDateTime.now());
    }

    public boolean needsReminder() {
        if (!isUpcoming() || reminderSent) {
            return false;
        }
        // Send reminder 24 hours before
        LocalDateTime reminderTime = getAppointmentDateTime().minusHours(24);
        return LocalDateTime.now().isAfter(reminderTime);
    }

    public boolean canReschedule() {
        return status.equals("PENDING") || status.equals("CONFIRMED");
    }

    public boolean canCancel() {
        return !status.equals("COMPLETED") && !status.equals("CANCELLED") && !status.equals("NO_SHOW");
    }
}
