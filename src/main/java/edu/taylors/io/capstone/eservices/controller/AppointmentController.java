package edu.taylors.io.capstone.eservices.controller;

import edu.taylors.io.capstone.eservices.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

 //Appointment Controller
 //Handles both Medical Clinic and International Office appointments

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    //Students Endpoints
    /**
     * GET /api/appointments/available-slots
     * Get available time slots
     * Query params: ?locationType=MEDICAL_CLINIC&locationName=Pantai&date=2026-02-20
     */
    @GetMapping("/available-slots")
    public ResponseEntity<?> getAvailableSlots(
            @RequestParam(required = false) String locationType,
            @RequestParam(required = false) String locationName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Map<String, Object>> slots = appointmentService.getAvailableSlots(locationType, locationName, date);
            return ResponseEntity.ok(Map.of(
                    "slots", slots,
                    "totalSlots", slots.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/appointments/book
     * Book appointment
     *
     * Request body:
     * {
     *   "appointmentType": "MEDICAL" or "OFFICE_CONSULTATION",
     *   "locationName": "Pantai Hospital" or "International Office",
     *   "locationAddress": "...",
     *   "locationPhone": "...",
     *   "date": "2026-02-20",
     *   "time": "10:00",
     *   "purpose": "Visa document review",
     *   "notes": "Special requirements"
     * }
     */
    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            String studentId = auth.getName();
            Map<String, Object> result = appointmentService.bookAppointment(studentId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/appointments/my-appointments
     * Get student's appointments
     * Query param: ?status=PENDING (optional)
     */
    @GetMapping("/my-appointments")
    public ResponseEntity<?> getMyAppointments(
            @RequestParam(required = false) String status,
            Authentication auth) {
        try {
            String studentId = auth.getName();
            List<Map<String, Object>> appointments = appointmentService.getMyAppointments(studentId, status);
            return ResponseEntity.ok(Map.of(
                    "appointments", appointments,
                    "total", appointments.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/appointments/upcoming
     * Get student's upcoming appointments
     */
    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingAppointments(Authentication auth) {
        try {
            String studentId = auth.getName();
            List<Map<String, Object>> appointments = appointmentService.getUpcomingAppointments(studentId);
            return ResponseEntity.ok(Map.of(
                    "appointments", appointments,
                    "total", appointments.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/appointments/{id}/confirm
     * Confirm appointment
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirmAppointment(
            @PathVariable Long id,
            Authentication auth) {
        try {
            String studentId = auth.getName();
            Map<String, Object> result = appointmentService.confirmAppointment(id, studentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/appointments/{id}/cancel
     * Cancel appointment
     *
     * Request body:
     * {
     *   "reason": "Cannot make it"
     * }
     */

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            String userId = auth.getName();
            String reason = (String) request.getOrDefault("reason", "");
            Map<String, Object> result = appointmentService.cancelAppointment(id, reason, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/appointments/{id}/reschedule
     * Reschedule appointment
     *
     * Request body:
     * {
     *   "newDate": "2026-02-25",
     *   "newTime": "14:00"
     * }
     */

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<?> rescheduleAppointment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            String userId = auth.getName();
            LocalDate newDate = LocalDate.parse(request.get("newDate").toString());
            LocalTime newTime = LocalTime.parse(request.get("newTime").toString());

            Map<String, Object> result = appointmentService.rescheduleAppointment(id, newDate, newTime, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    //Staff Endpoints

    /**
     * GET /api/appointments/staff/all
     * Get all appointments
     * Query params: ?status=PENDING&type=MEDICAL&date=2026-02-20
     */
    @GetMapping("/staff/all")
    public ResponseEntity<?> getAllAppointments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Map<String, Object>> appointments = appointmentService.getAllAppointments(status, type, date);
            return ResponseEntity.ok(Map.of(
                    "appointments", appointments,
                    "total", appointments.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/appointments/staff/{id}/status
     * Update appointment status
     *
     * Request body:
     * {
     *   "status": "COMPLETED",
     *   "notes": "Student attended on time"
     * }
     */
    @PutMapping("/staff/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            String staffId = auth.getName();
            String newStatus = (String) request.get("status");
            String notes = (String) request.getOrDefault("notes", "");

            Map<String, Object> result = appointmentService.updateAppointmentStatus(id, newStatus, notes, staffId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/appointments/staff/create-slots
     * Create time slots
     *
     * Request body:
     * {
     *   "locationType": "INTERNATIONAL_OFFICE",
     *   "locationName": "International Office",
     *   "startDate": "2026-02-20",
     *   "endDate": "2026-02-28",
     *   "startTime": "09:00",
     *   "endTime": "17:00",
     *   "slotDuration": 30
     * }
     */
    @PostMapping("/staff/create-slots")
    public ResponseEntity<?> createTimeSlots(
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            Map<String, Object> result = appointmentService.createTimeSlots(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/appointments/staff/statistics
     * Get appointment statistics
     */
    @GetMapping("/staff/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> stats = appointmentService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
