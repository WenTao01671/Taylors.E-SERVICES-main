package edu.taylors.io.capstone.eservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * TimeSlot entity - manages available appointment slots
 * Works for both medical clinics and International Office
 */
@Entity
@Table(name = "time_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Location
    private String locationType;  // MEDICAL_CLINIC or INTERNATIONAL_OFFICE
    private String locationName;  // "Pantai Hospital" or "International Office"
    private String roomNumber;    // For office: "Room 301, Block A"

    // Staff (for office appointments)
    @ManyToOne
    @JoinColumn(name = "staff_id")
    private User staff;  // Which staff member is available

    // Date & Time
    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    // Capacity
    private Integer maxAppointments;  // How many slots available at this time
    private Integer bookedAppointments;  // How many are booked

    // Availability
    @Column(nullable = false)
    private Boolean isAvailable;

    // Purpose limitation
    private String appointmentType;  // null = any type, or specific: MEDICAL, VISA_INTERVIEW, etc.

    // Notes
    private String notes;  // "Staff meeting 2-3pm", "Lunch break", etc.

    @PrePersist
    protected void onCreate() {
        if (bookedAppointments == null) {
            bookedAppointments = 0;
        }
        if (maxAppointments == null) {
            maxAppointments = 1;
        }
        if (isAvailable == null) {
            isAvailable = true;
        }
    }

    // Helper methods
    public boolean hasAvailableSlots() {
        return isAvailable && bookedAppointments < maxAppointments;
    }

    public void bookSlot() {
        if (!hasAvailableSlots()) {
            throw new RuntimeException("No available slots");
        }
        bookedAppointments++;
        if (bookedAppointments >= maxAppointments) {
            isAvailable = false;
        }
    }

    public void releaseSlot() {
        if (bookedAppointments > 0) {
            bookedAppointments--;
            isAvailable = true;
        }
    }

    public Integer getAvailableSlots() {
        return maxAppointments - bookedAppointments;
    }
}
