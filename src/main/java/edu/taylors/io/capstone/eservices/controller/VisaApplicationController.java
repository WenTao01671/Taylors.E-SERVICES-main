package edu.taylors.io.capstone.eservices.controller;

import edu.taylors.io.capstone.eservices.service.VisaApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Visa Application Controller
 * Handles visa processing, tracking, and EMGS integration
 */
@RestController
@RequestMapping("/api/visa")
@RequiredArgsConstructor
public class VisaApplicationController {

    private final VisaApplicationService visaService;

    //STUDENT ENDPOINTS

    /**
     * GET /api/visa/my-application
     * Get student's visa application
     */
    @GetMapping("/my-application")
    public ResponseEntity<?> getMyVisaApplication(Authentication auth) {
        try {
            String studentId = auth.getName();
            Map<String, Object> application = visaService.getVisaApplication(studentId);
            return ResponseEntity.ok(application);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/visa/create
     * Create new visa application with program details
     *
     * Request body:
     * {
     *   "visaType": "STUDENT_PASS",
     *   "passportNumber": "A12345678",
     *   "passportExpiry": "2028-12-31T00:00:00",
     *   "nationality": "Sudan",
     *   "programName": "Bachelor of Software Engineering",
     *   "faculty": "School of Computer Science",
     *   "programStartDate": "2026-09-01T00:00:00"
     * }
     */
    @PostMapping("/create")
    public ResponseEntity<?> createVisaApplication(
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            String studentId = auth.getName();
            Map<String, Object> result = visaService.createVisaApplication(studentId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/visa/submit-documents
     * Mark documents as submitted
     */
    @PostMapping("/submit-documents")
    public ResponseEntity<?> submitDocuments(Authentication auth) {
        try {
            String studentId = auth.getName();
            Map<String, Object> result = visaService.submitDocuments(studentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/visa/timeline
     * Get visa processing timeline
     */
    @GetMapping("/timeline")
    public ResponseEntity<?> getVisaTimeline(Authentication auth) {
        try {
            String studentId = auth.getName();
            Map<String, Object> timeline = visaService.getVisaTimeline(studentId);
            return ResponseEntity.ok(timeline);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    //STAFF ENDPOINTS

    /**
     * GET /api/visa/staff/all
     * Get all visa applications
     * Query param: ?status=EMGS_PROCESSING (optional)
     */
    @GetMapping("/staff/all")
    public ResponseEntity<?> getAllVisaApplications(
            @RequestParam(required = false) String status) {
        try {
            List<Map<String, Object>> applications = visaService.getAllVisaApplications(status);
            return ResponseEntity.ok(Map.of(
                    "applications", applications,
                    "total", applications.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/visa/staff/{id}/submit-emgs
     * Submit visa application to EMGS
     */
    @PostMapping("/staff/{id}/submit-emgs")
    public ResponseEntity<?> submitToEmgs(
            @PathVariable Long id,
            Authentication auth) {
        try {
            String staffId = auth.getName();
            Map<String, Object> result = visaService.submitToEmgs(id, staffId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/visa/staff/{id}/status
     * Update visa application status
     *
     * Request body:
     * {
     *   "status": "EMGS_APPROVED",
     *   "notes": "EMGS approval received on 15 Feb 2026"
     * }
     */
    @PutMapping("/staff/{id}/status")
    public ResponseEntity<?> updateVisaStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            String staffId = auth.getName();
            String newStatus = (String) request.get("status");
            String notes = (String) request.getOrDefault("notes", "");

            Map<String, Object> result = visaService.updateEmgsStatus(id, newStatus, notes, staffId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/visa/staff/statistics
     * Get visa application statistics
     */
    @GetMapping("/staff/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> stats = visaService.getVisaStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
