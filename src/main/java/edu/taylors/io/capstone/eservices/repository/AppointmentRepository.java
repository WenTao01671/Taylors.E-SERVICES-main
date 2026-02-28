package edu.taylors.io.capstone.eservices.repository;

import edu.taylors.io.capstone.eservices.entity.Appointment;
import edu.taylors.io.capstone.eservices.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByAppointmentNumber(String appointmentNumber);

    // Find by student
    List<Appointment> findByStudentOrderByAppointmentDateDescAppointmentTimeDesc(User student);

    List<Appointment> findByStudentStudentIdOrderByAppointmentDateDescAppointmentTimeDesc(String studentId);

    // Find by type
    List<Appointment> findByAppointmentTypeOrderByAppointmentDateDescAppointmentTimeDesc(String appointmentType);

    // Find by status
    List<Appointment> findByStatusOrderByAppointmentDateDescAppointmentTimeDesc(String status);

    // Find by staff (for office appointments)
    List<Appointment> findByAssignedStaffOrderByAppointmentDateDescAppointmentTimeDesc(User staff);

    // Find upcoming appointments
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate >= :today " +
            "AND a.status IN ('PENDING', 'CONFIRMED') " +
            "ORDER BY a.appointmentDate ASC, a.appointmentTime ASC")
    List<Appointment> findUpcomingAppointments(@Param("today") LocalDate today);

    // Find appointments for a specific date
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date " +
            "ORDER BY a.appointmentTime ASC")
    List<Appointment> findByDate(@Param("date") LocalDate date);

    // Find appointments needing reminder
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :tomorrow " +
            "AND a.status IN ('PENDING', 'CONFIRMED') " +
            "AND (a.reminderSent = false OR a.reminderSent IS NULL)")
    List<Appointment> findAppointmentsNeedingReminder(@Param("tomorrow") LocalDate tomorrow);

    // Find past appointments not marked as completed
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate < :today " +
            "AND a.status NOT IN ('COMPLETED', 'CANCELLED', 'NO_SHOW')")
    List<Appointment> findPastIncompleteAppointments(@Param("today") LocalDate today);

    // Count appointments by type
    Long countByAppointmentType(String appointmentType);

    // Count by status
    Long countByStatus(String status);

    // Find student's upcoming appointments
    @Query("SELECT a FROM Appointment a WHERE a.student.studentId = :studentId " +
            "AND a.appointmentDate >= :today " +
            "AND a.status IN ('PENDING', 'CONFIRMED') " +
            "ORDER BY a.appointmentDate ASC, a.appointmentTime ASC")
    List<Appointment> findStudentUpcomingAppointments(
            @Param("studentId") String studentId,
            @Param("today") LocalDate today
    );

    // Find staff's appointments for today
    @Query("SELECT a FROM Appointment a WHERE a.assignedStaff.id = :staffId " +
            "AND a.appointmentDate = :today " +
            "ORDER BY a.appointmentTime ASC")
    List<Appointment> findStaffTodayAppointments(
            @Param("staffId") Long staffId,
            @Param("today") LocalDate today
    );
}
