package edu.taylors.io.capstone.eservices.service;

import edu.taylors.io.capstone.eservices.entity.Appointment;
import edu.taylors.io.capstone.eservices.entity.TimeSlot;
import edu.taylors.io.capstone.eservices.entity.User;
import edu.taylors.io.capstone.eservices.repository.AppointmentRepository;
import edu.taylors.io.capstone.eservices.repository.TimeSlotRepository;
import edu.taylors.io.capstone.eservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Get available time slots
     */
    public List<Map<String, Object>> getAvailableSlots(String locationType, String locationName, LocalDate date) {
        List<TimeSlot> slots;

        if (locationName != null && !locationName.isEmpty()) {
            slots = timeSlotRepository.findAvailableSlots(locationName, date);
        } else if (locationType != null && !locationType.isEmpty()) {
            LocalDate endDate = date.plusDays(7); // Next 7 days
            slots = timeSlotRepository.findAvailableSlotsByType(locationType, date, endDate);
        } else {
            slots = timeSlotRepository.findAll().stream()
                    .filter(TimeSlot::hasAvailableSlots)
                    .collect(Collectors.toList());
        }

        return slots.stream()
                .map(this::convertSlotToMap)
                .collect(Collectors.toList());
    }

    /**
     * Book appointment
     */
    @Transactional
    public Map<String, Object> bookAppointment(String studentId, Map<String, Object> request) {
        User student = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Parse request
        String appointmentType = (String) request.get("appointmentType");
        String locationName = (String) request.get("locationName");
        LocalDate date = LocalDate.parse(request.get("date").toString());
        LocalTime time = LocalTime.parse(request.get("time").toString());
        String purpose = (String) request.getOrDefault("purpose", "");
        String notes = (String) request.getOrDefault("notes", "");

        // Find time slot
        TimeSlot slot = timeSlotRepository.findByLocationNameAndDateAndStartTime(locationName, date, time)
                .orElseThrow(() -> new RuntimeException("Time slot not available"));

        if (!slot.hasAvailableSlots()) {
            throw new RuntimeException("This time slot is fully booked");
        }

        // Create appointment
        Appointment appointment = Appointment.builder()
                .student(student)
                .appointmentNumber(generateAppointmentNumber())
                .appointmentType(appointmentType)
                .locationName(locationName)
                .locationAddress((String) request.get("locationAddress"))
                .locationPhone((String) request.get("locationPhone"))
                .roomNumber(slot.getRoomNumber())
                .appointmentDate(date)
                .appointmentTime(time)
                .durationMinutes(slot.getEndTime().getHour() * 60 + slot.getEndTime().getMinute() -
                        (slot.getStartTime().getHour() * 60 + slot.getStartTime().getMinute()))
                .assignedStaff(slot.getStaff())
                .status("PENDING")
                .purpose(purpose)
                .studentNotes(notes)
                .createdBy(studentId)
                .build();

        // Book the slot
        slot.bookSlot();
        timeSlotRepository.save(slot);

        appointment = appointmentRepository.save(appointment);
        log.info("Appointment {} booked by student {}", appointment.getAppointmentNumber(), studentId);

        // Send confirmation email
        sendAppointmentConfirmationEmail(student, appointment);

        return Map.of(
                "message", "Appointment booked successfully",
                "appointment", convertToMap(appointment)
        );
    }

    /**
     * Get student's appointments
     */
    public List<Map<String, Object>> getMyAppointments(String studentId, String status) {
        List<Appointment> appointments;

        if (status != null && !status.isEmpty()) {
            User student = userRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            appointments = appointmentRepository.findByStudentOrderByAppointmentDateDescAppointmentTimeDesc(student)
                    .stream()
                    .filter(a -> a.getStatus().equals(status))
                    .collect(Collectors.toList());
        } else {
            appointments = appointmentRepository.findByStudentStudentIdOrderByAppointmentDateDescAppointmentTimeDesc(studentId);
        }

        return appointments.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    /**
     * Get student's upcoming appointments
     */
    public List<Map<String, Object>> getUpcomingAppointments(String studentId) {
        List<Appointment> appointments = appointmentRepository.findStudentUpcomingAppointments(
                studentId,
                LocalDate.now()
        );

        return appointments.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    /**
     * Confirm appointment
     */
    @Transactional
    public Map<String, Object> confirmAppointment(Long appointmentId, String studentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getStudent().getStudentId().equals(studentId)) {
            throw new RuntimeException("Not authorized");
        }

        appointment.setStatus("CONFIRMED");
        appointment.setConfirmedByStudent(true);
        appointment.setConfirmationDate(LocalDateTime.now());

        appointmentRepository.save(appointment);

        return Map.of(
                "message", "Appointment confirmed",
                "appointment", convertToMap(appointment)
        );
    }

    /**
     * Cancel appointment
     */
    @Transactional
    public Map<String, Object> cancelAppointment(Long appointmentId, String reason, String userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.canCancel()) {
            throw new RuntimeException("Cannot cancel this appointment");
        }

        // Release the time slot
        TimeSlot slot = timeSlotRepository.findByLocationNameAndDateAndStartTime(
                appointment.getLocationName(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime()
        ).orElse(null);

        if (slot != null) {
            slot.releaseSlot();
            timeSlotRepository.save(slot);
        }

        appointment.setStatus("CANCELLED");
        appointment.setCancellationReason(reason);

        appointmentRepository.save(appointment);

        // Send cancellation email
        sendCancellationEmail(appointment.getStudent(), appointment);

        return Map.of(
                "message", "Appointment cancelled successfully",
                "appointment", convertToMap(appointment)
        );
    }

    /**
     * Reschedule appointment
     */
    @Transactional
    public Map<String, Object> rescheduleAppointment(Long appointmentId, LocalDate newDate, LocalTime newTime, String userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.canReschedule()) {
            throw new RuntimeException("Cannot reschedule this appointment");
        }

        // Release old slot
        TimeSlot oldSlot = timeSlotRepository.findByLocationNameAndDateAndStartTime(
                appointment.getLocationName(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime()
        ).orElse(null);

        if (oldSlot != null) {
            oldSlot.releaseSlot();
            timeSlotRepository.save(oldSlot);
        }

        // Book new slot
        TimeSlot newSlot = timeSlotRepository.findByLocationNameAndDateAndStartTime(
                appointment.getLocationName(),
                newDate,
                newTime
        ).orElseThrow(() -> new RuntimeException("New time slot not available"));

        if (!newSlot.hasAvailableSlots()) {
            throw new RuntimeException("New time slot is fully booked");
        }

        newSlot.bookSlot();
        timeSlotRepository.save(newSlot);

        // Update appointment
        if (appointment.getOriginalDate() == null) {
            appointment.setOriginalDate(LocalDateTime.of(appointment.getAppointmentDate(), appointment.getAppointmentTime()));
        }
        appointment.setAppointmentDate(newDate);
        appointment.setAppointmentTime(newTime);
        appointment.setStatus("RESCHEDULED");
        appointment.setRescheduleCount(appointment.getRescheduleCount() + 1);

        appointmentRepository.save(appointment);

        // Send notification
        sendRescheduleEmail(appointment.getStudent(), appointment);

        return Map.of(
                "message", "Appointment rescheduled successfully",
                "appointment", convertToMap(appointment)
        );
    }

    // ==================== STAFF ENDPOINTS ====================

    /**
     * Get all appointments (Staff)
     */
    public List<Map<String, Object>> getAllAppointments(String status, String type, LocalDate date) {
        List<Appointment> appointments;

        if (status != null && !status.isEmpty()) {
            appointments = appointmentRepository.findByStatusOrderByAppointmentDateDescAppointmentTimeDesc(status);
        } else if (type != null && !type.isEmpty()) {
            appointments = appointmentRepository.findByAppointmentTypeOrderByAppointmentDateDescAppointmentTimeDesc(type);
        } else if (date != null) {
            appointments = appointmentRepository.findByDate(date);
        } else {
            appointments = appointmentRepository.findAll();
        }

        return appointments.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    /**
     * Update appointment status (Staff)
     */
    @Transactional
    public Map<String, Object> updateAppointmentStatus(Long appointmentId, String newStatus, String notes, String staffId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointment.setStatus(newStatus);
        appointment.setStaffNotes(notes);

        appointmentRepository.save(appointment);

        return Map.of(
                "message", "Appointment status updated",
                "appointment", convertToMap(appointment)
        );
    }

    /**
     * Create time slots (Staff)
     */
    @Transactional
    public Map<String, Object> createTimeSlots(Map<String, Object> request) {
        String locationType = (String) request.get("locationType");
        String locationName = (String) request.get("locationName");
        LocalDate startDate = LocalDate.parse(request.get("startDate").toString());
        LocalDate endDate = LocalDate.parse(request.get("endDate").toString());
        LocalTime startTime = LocalTime.parse(request.get("startTime").toString());
        LocalTime endTime = LocalTime.parse(request.get("endTime").toString());
        Integer slotDuration = (Integer) request.getOrDefault("slotDuration", 30); // minutes

        int slotsCreated = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalTime currentTime = startTime;

            while (currentTime.isBefore(endTime)) {
                LocalTime slotEnd = currentTime.plusMinutes(slotDuration);
                if (slotEnd.isAfter(endTime)) break;

                TimeSlot slot = TimeSlot.builder()
                        .locationType(locationType)
                        .locationName(locationName)
                        .date(date)
                        .startTime(currentTime)
                        .endTime(slotEnd)
                        .maxAppointments(1)
                        .bookedAppointments(0)
                        .isAvailable(true)
                        .build();

                timeSlotRepository.save(slot);
                slotsCreated++;

                currentTime = slotEnd;
            }
        }

        log.info("Created {} time slots for {}", slotsCreated, locationName);

        return Map.of(
                "message", "Time slots created successfully",
                "slotsCreated", slotsCreated
        );
    }

    /**
     * Get appointment statistics
     */
    public Map<String, Object> getStatistics() {
        long total = appointmentRepository.count();
        long pending = appointmentRepository.countByStatus("PENDING");
        long confirmed = appointmentRepository.countByStatus("CONFIRMED");
        long completed = appointmentRepository.countByStatus("COMPLETED");
        long cancelled = appointmentRepository.countByStatus("CANCELLED");
        long noShow = appointmentRepository.countByStatus("NO_SHOW");

        long medical = appointmentRepository.countByAppointmentType("MEDICAL");
        long office = appointmentRepository.countByAppointmentType("OFFICE_CONSULTATION");

        return Map.of(
                "totalAppointments", total,
                "pending", pending,
                "confirmed", confirmed,
                "completed", completed,
                "cancelled", cancelled,
                "noShow", noShow,
                "medicalAppointments", medical,
                "officeAppointments", office
        );
    }

    /**
     * Send appointment reminders (Scheduled job)
     */
    @Transactional
    public void sendAppointmentReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Appointment> appointments = appointmentRepository.findAppointmentsNeedingReminder(tomorrow);

        for (Appointment appointment : appointments) {
            sendReminderEmail(appointment.getStudent(), appointment);
            appointment.setReminderSent(true);
            appointment.setReminderSentDate(LocalDateTime.now());
            appointment.setReminderCount(appointment.getReminderCount() + 1);
            appointmentRepository.save(appointment);
        }

        log.info("Sent {} appointment reminders", appointments.size());
    }

    // Helper methods

    private String generateAppointmentNumber() {
        int year = LocalDateTime.now().getYear();
        long count = appointmentRepository.count() + 1;
        return String.format("APT-%d-%05d", year, count);
    }

    private Map<String, Object> convertToMap(Appointment appointment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", appointment.getId());
        map.put("appointmentNumber", appointment.getAppointmentNumber());
        map.put("studentId", appointment.getStudent().getStudentId());
        map.put("studentName", appointment.getStudent().getFullName());
        map.put("appointmentType", appointment.getAppointmentType());
        map.put("locationName", appointment.getLocationName());
        map.put("locationAddress", appointment.getLocationAddress());
        map.put("roomNumber", appointment.getRoomNumber());
        map.put("appointmentDate", appointment.getAppointmentDate());
        map.put("appointmentTime", appointment.getAppointmentTime());
        map.put("durationMinutes", appointment.getDurationMinutes());
        map.put("status", appointment.getStatus());
        map.put("purpose", appointment.getPurpose());
        map.put("confirmedByStudent", appointment.getConfirmedByStudent());
        map.put("assignedStaff", appointment.getAssignedStaff() != null ?
                appointment.getAssignedStaff().getFullName() : null);
        map.put("createdAt", appointment.getCreatedAt());
        return map;
    }

    private Map<String, Object> convertSlotToMap(TimeSlot slot) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", slot.getId());
        map.put("locationType", slot.getLocationType());
        map.put("locationName", slot.getLocationName());
        map.put("roomNumber", slot.getRoomNumber());
        map.put("date", slot.getDate());
        map.put("startTime", slot.getStartTime());
        map.put("endTime", slot.getEndTime());
        map.put("availableSlots", slot.getAvailableSlots());
        map.put("staff", slot.getStaff() != null ? slot.getStaff().getFullName() : null);
        return map;
    }

    // Email notifications

    private void sendAppointmentConfirmationEmail(User student, Appointment appointment) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        String subject = "Appointment Booked - " + appointment.getAppointmentType();
        String message = String.format(
                "Dear %s,\n\n" +
                        "Your appointment has been booked successfully.\n\n" +
                        "Appointment Details:\n" +
                        "Number: %s\n" +
                        "Type: %s\n" +
                        "Location: %s\n" +
                        "Room: %s\n" +
                        "Date: %s\n" +
                        "Time: %s\n" +
                        "Duration: %d minutes\n\n" +
                        "Purpose: %s\n\n" +
                        "Please confirm your appointment by clicking the link in your student portal.\n" +
                        "You will receive a reminder 24 hours before your appointment.\n\n" +
                        "Best regards,\n" +
                        "Taylor's University",
                student.getFullName(),
                appointment.getAppointmentNumber(),
                appointment.getAppointmentType(),
                appointment.getLocationName(),
                appointment.getRoomNumber() != null ? appointment.getRoomNumber() : "N/A",
                appointment.getAppointmentDate().format(dateFormatter),
                appointment.getAppointmentTime().format(timeFormatter),
                appointment.getDurationMinutes(),
                appointment.getPurpose()
        );

        emailService.sendEmail(student.getEmail(), subject, message);
    }

    private void sendReminderEmail(User student, Appointment appointment) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        String subject = "Reminder: Appointment Tomorrow";
        String message = String.format(
                "Dear %s,\n\n" +
                        "This is a reminder of your appointment tomorrow:\n\n" +
                        "Type: %s\n" +
                        "Location: %s\n" +
                        "Room: %s\n" +
                        "Date: %s\n" +
                        "Time: %s\n\n" +
                        "Please arrive 10 minutes early.\n\n" +
                        "Best regards,\n" +
                        "Taylor's University",
                student.getFullName(),
                appointment.getAppointmentType(),
                appointment.getLocationName(),
                appointment.getRoomNumber() != null ? appointment.getRoomNumber() : "N/A",
                appointment.getAppointmentDate().format(dateFormatter),
                appointment.getAppointmentTime().format(timeFormatter)
        );

        emailService.sendEmail(student.getEmail(), subject, message);
    }

    private void sendCancellationEmail(User student, Appointment appointment) {
        String subject = "Appointment Cancelled";
        String message = String.format(
                "Dear %s,\n\n" +
                        "Your appointment has been cancelled.\n\n" +
                        "Appointment Number: %s\n" +
                        "Reason: %s\n\n" +
                        "You can book a new appointment through the student portal.\n\n" +
                        "Best regards,\n" +
                        "Taylor's University",
                student.getFullName(),
                appointment.getAppointmentNumber(),
                appointment.getCancellationReason()
        );

        emailService.sendEmail(student.getEmail(), subject, message);
    }

    private void sendRescheduleEmail(User student, Appointment appointment) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        String subject = "Appointment Rescheduled";
        String message = String.format(
                "Dear %s,\n\n" +
                        "Your appointment has been rescheduled.\n\n" +
                        "New Date: %s\n" +
                        "New Time: %s\n" +
                        "Location: %s\n\n" +
                        "Best regards,\n" +
                        "Taylor's University",
                student.getFullName(),
                appointment.getAppointmentDate().format(dateFormatter),
                appointment.getAppointmentTime().format(timeFormatter),
                appointment.getLocationName()
        );

        emailService.sendEmail(student.getEmail(), subject, message);
    }
}
