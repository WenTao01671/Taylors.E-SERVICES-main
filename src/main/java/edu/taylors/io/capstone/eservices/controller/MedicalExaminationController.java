package edu.taylors.io.capstone.eservices.controller;

import edu.taylors.io.capstone.eservices.service.MedicalExaminationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Medical Examination Controller
 *
 * ADD THESE ENDPOINTS TO YOUR EXISTING CONTROLLER OR CREATE A NEW ONE
 */
@RestController
@RequestMapping("/api/medical")
@RequiredArgsConstructor
public class MedicalExaminationController {

    private final MedicalExaminationService medicalService;

    //STUDENT ENDPOINTS

    /**
     * GET /api/medical/my-examination
     * Get student's medical examination
     */
    @GetMapping("/my-examination")
    public ResponseEntity<?> getMyMedicalExamination(Authentication auth) {
        try {
            String studentId = auth.getName();
            Map<String, Object> examination = medicalService.getMedicalExamination(studentId);
            return ResponseEntity.ok(examination);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/medical/book-appointment
     * Book medical appointment
     *
     * Request body:
     * {
     *   "appointmentDate": "2026-02-20T10:00:00",
     *   "clinicName": "Pantai Hospital KL",
     *   "clinicAddress": "8, Jalan Bukit Pantai, KL",
     *   "clinicPhone": "+603-22967788"
     * }
     */
    @PostMapping("/book-appointment")
    public ResponseEntity<?> bookAppointment(
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            String studentId = auth.getName();
            Map<String, Object> result = medicalService.bookAppointment(studentId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/medical/upload-document
     * Upload medical document
     */
    @PostMapping("/upload-document")
    public ResponseEntity<?> uploadMedicalDocument(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }

            String studentId = auth.getName();
            Map<String, Object> result = medicalService.uploadDocument(studentId, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    //STAFF ENDPOINTS

    /**
     * GET /api/medical/staff/all
     * Get all medical examinations
     * Query param: ?status=PENDING (optional)
     */
    @GetMapping("/staff/all")
    public ResponseEntity<?> getAllExaminations(
            @RequestParam(required = false) String status) {
        try {
            List<Map<String, Object>> examinations = medicalService.getAllExaminations(status);
            return ResponseEntity.ok(Map.of(
                    "examinations", examinations,
                    "total", examinations.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/medical/staff/{id}/tests
     * Update test results
     *
     * Request body:
     * {
     *   "chestXrayDone": true,
     *   "bloodTestDone": true,
     *   "urineTestDone": false
     * }
     */
    @PutMapping("/staff/{id}/tests")
    public ResponseEntity<?> updateTests(
            @PathVariable Long id,
            @RequestBody Map<String, Object> tests,
            Authentication auth) {
        try {
            String staffId = auth.getName();
            Map<String, Object> result = medicalService.updateTests(id, tests, staffId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/medical/staff/{id}/submit-results
     * Submit final medical results
     *
     * Request body:
     * {
     *   "passed": true,
     *   "notes": "All tests clear"
     * }
     */
    @PostMapping("/staff/{id}/submit-results")
    public ResponseEntity<?> submitResults(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            String staffId = auth.getName();
            boolean passed = (Boolean) request.get("passed");
            String notes = (String) request.getOrDefault("notes", "");

            Map<String, Object> result = medicalService.submitResults(id, passed, notes, staffId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/medical/staff/{id}/submit-emgs
     * Submit to EMGS
     */
    @PostMapping("/staff/{id}/submit-emgs")
    public ResponseEntity<?> submitToEmgs(
            @PathVariable Long id,
            Authentication auth) {
        try {
            String staffId = auth.getName();
            Map<String, Object> result = medicalService.submitToEmgs(id, staffId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/medical/staff/statistics
     * Get medical statistics
     */
    @GetMapping("/staff/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> stats = medicalService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
