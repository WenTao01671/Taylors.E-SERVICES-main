package edu.taylors.io.capstone.eservices.repository;

import edu.taylors.io.capstone.eservices.entity.TimeSlot;
import edu.taylors.io.capstone.eservices.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    // Find available slots by location and date
    @Query("SELECT t FROM TimeSlot t WHERE t.locationName = :locationName " +
            "AND t.date = :date " +
            "AND t.isAvailable = true " +
            "ORDER BY t.startTime ASC")
    List<TimeSlot> findAvailableSlots(
            @Param("locationName") String locationName,
            @Param("date") LocalDate date
    );

    // Find available slots by type and date range
    @Query("SELECT t FROM TimeSlot t WHERE t.locationType = :locationType " +
            "AND t.date BETWEEN :startDate AND :endDate " +
            "AND t.isAvailable = true " +
            "ORDER BY t.date ASC, t.startTime ASC")
    List<TimeSlot> findAvailableSlotsByType(
            @Param("locationType") String locationType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find slots by staff and date
    @Query("SELECT t FROM TimeSlot t WHERE t.staff.id = :staffId " +
            "AND t.date = :date " +
            "ORDER BY t.startTime ASC")
    List<TimeSlot> findStaffSlots(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date
    );

    // Find specific slot
    Optional<TimeSlot> findByLocationNameAndDateAndStartTime(
            String locationName,
            LocalDate date,
            LocalTime startTime
    );

    // Find all slots for International Office
    @Query("SELECT t FROM TimeSlot t WHERE t.locationType = 'INTERNATIONAL_OFFICE' " +
            "AND t.date >= :startDate " +
            "AND t.isAvailable = true " +
            "ORDER BY t.date ASC, t.startTime ASC")
    List<TimeSlot> findOfficeAvailableSlots(@Param("startDate") LocalDate startDate);

    // Find all slots for Medical Clinics
    @Query("SELECT t FROM TimeSlot t WHERE t.locationType = 'MEDICAL_CLINIC' " +
            "AND t.date >= :startDate " +
            "AND t.isAvailable = true " +
            "ORDER BY t.date ASC, t.startTime ASC")
    List<TimeSlot> findMedicalAvailableSlots(@Param("startDate") LocalDate startDate);
}
