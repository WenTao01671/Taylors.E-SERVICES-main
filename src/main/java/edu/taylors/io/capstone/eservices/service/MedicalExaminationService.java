package edu.taylors.io.capstone.eservices.service;

import edu.taylors.io.capstone.eservices.entity.FileCategory;
import edu.taylors.io.capstone.eservices.entity.MedicalExamination;
import edu.taylors.io.capstone.eservices.entity.StudentFile;
import edu.taylors.io.capstone.eservices.entity.User;
import edu.taylors.io.capstone.eservices.repository.MedicalExaminationRepository;
import edu.taylors.io.capstone.eservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalExaminationService {

    private final MedicalExaminationRepository medicalRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;

    /**
     * Get or create medical examination for student
     */
    @Transactional
    public Map<String, Object> getMedicalExamination(String studentId) {
        User student = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        MedicalExamination exam = medicalRepository.findByStudent(student)
                .orElseGet(() -> createNewExamination(student));

        return convertToMap(exam);
    }

    /**
     * Book medical appointment
     */
    @Transactional
    public Map<String, Object> bookAppointment(String studentId, Map<String, Object> request) {
        User student = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        MedicalExamination exam = medicalRepository.findByStudent(student)
                .orElseGet(() -> createNewExamination(student));

        // Update appointment details
        exam.setAppointmentDate(LocalDateTime.parse(request.get("appointmentDate").toString()));
        exam.setClinicName((String) request.get("clinicName"));
        exam.setClinicAddress((String) request.get("clinicAddress"));
        exam.setClinicPhone((String) request.get("clinicPhone"));
        exam.setStatus("SCHEDULED");
        exam.updateProgress();

        medicalRepository.save(exam);

        // Send confirmation email
        sendAppointmentEmail(student, exam);

        return Map.of(
                "message", "Appointment booked successfully",
                "examination", convertToMap(exam)
        );
    }

    /**
     * Update test results
     */
    @Transactional
    public Map<String, Object> updateTests(Long id, Map<String, Object> tests, String staffId) {
        MedicalExamination exam = medicalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Examination not found"));

        if (tests.containsKey("chestXrayDone")) {
            exam.setChestXrayDone((Boolean) tests.get("chestXrayDone"));
        }
        if (tests.containsKey("bloodTestDone")) {
            exam.setBloodTestDone((Boolean) tests.get("bloodTestDone"));
        }
        if (tests.containsKey("urineTestDone")) {
            exam.setUrineTestDone((Boolean) tests.get("urineTestDone"));
        }

        if (exam.allTestsCompleted()) {
            exam.setStatus("COMPLETED");
        }

        exam.updateProgress();
        medicalRepository.save(exam);

        return Map.of("message", "Tests updated", "examination", convertToMap(exam));
    }

    /**
     * Submit medical results
     */
    @Transactional
    public Map<String, Object> submitResults(Long id, boolean passed, String notes, String staffId) {
        MedicalExamination exam = medicalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Examination not found"));

        exam.setResultDate(LocalDateTime.now());
        exam.setPassed(passed);
        exam.setResultNotes(notes);

        if (passed) {
            exam.setStatus("PASSED");
            exam.setExpiryDate(LocalDateTime.now().plusMonths(3)); // Valid 3 months
            sendPassedEmail(exam.getStudent(), exam);
        } else {
            exam.setStatus("FAILED");
            sendFailedEmail(exam.getStudent(), exam);
        }

        exam.updateProgress();
        medicalRepository.save(exam);

        return Map.of(
                "message", passed ? "Medical examination passed" : "Medical examination failed",
                "examination", convertToMap(exam)
        );
    }

    /**
     * Submit to EMGS
     */
    @Transactional
    public Map<String, Object> submitToEmgs(Long id, String staffId) {
        MedicalExamination exam = medicalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Examination not found"));

        if (!Boolean.TRUE.equals(exam.getPassed())) {
            throw new RuntimeException("Only passed examinations can be submitted to EMGS");
        }

        exam.setSubmittedToEmgs(true);
        exam.setEmgsSubmissionDate(LocalDateTime.now());
        exam.setEmgsReference("EMGS-MED-" + System.currentTimeMillis());
        exam.setStatus("SUBMITTED_TO_EMGS");
        exam.updateProgress();

        medicalRepository.save(exam);

        sendEmgsSubmissionEmail(exam.getStudent(), exam);

        return Map.of(
                "message", "Successfully submitted to EMGS",
                "emgsReference", exam.getEmgsReference(),
                "examination", convertToMap(exam)
        );
    }

    /**
     * Upload medical document
     */
    @Transactional
    public Map<String, Object> uploadDocument(String studentId, MultipartFile file) {
        User student = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        MedicalExamination exam = medicalRepository.findByStudent(student)
                .orElseGet(() -> createNewExamination(student));

        try {
            // Upload file using your existing FileStorageService
            fileStorageService.uploadFile(studentId, file, FileCategory.MEDICAL, "Medical Report");

            medicalRepository.save(exam);

            return Map.of(
                    "message", "Document uploaded successfully",
                    "fileName", file.getOriginalFilename()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document: " + e.getMessage());
        }

        //return Map.of(
         //       "message", "Document uploaded successfully",
           //     "fileName", file.getOriginalFilename()
        //);
    }

    /**
     * Get all examinations (Staff only)
     */
    public List<Map<String, Object>> getAllExaminations(String status) {
        List<MedicalExamination> exams;

        if (status != null && !status.isEmpty()) {
            exams = medicalRepository.findByStatus(status);
        } else {
            exams = medicalRepository.findAll();
        }

        return exams.stream()
                .map(this::convertToMap)
                .toList();
    }

    /**
     * Get statistics (Staff only)
     */
    public Map<String, Object> getStatistics() {
        long total = medicalRepository.count();
        long pending = medicalRepository.findByStatus("PENDING").size();
        long scheduled = medicalRepository.findByStatus("SCHEDULED").size();
        long completed = medicalRepository.findByStatus("COMPLETED").size();
        long passed = medicalRepository.findByStatus("PASSED").size();
        long failed = medicalRepository.findByStatus("FAILED").size();
        long submittedToEmgs = medicalRepository.findByStatus("SUBMITTED_TO_EMGS").size();

        return Map.of(
                "totalExaminations", total,
                "pending", pending,
                "scheduled", scheduled,
                "completed", completed,
                "passed", passed,
                "failed", failed,
                "submittedToEmgs", submittedToEmgs
        );
    }

    // Private helper methods

    private MedicalExamination createNewExamination(User student) {
        MedicalExamination exam = MedicalExamination.builder()
                .student(student)
                .examinationNumber(generateExamNumber())
                .status("PENDING")
                .progressPercentage(0)
                .submittedToEmgs(false)
                .build();

        exam = medicalRepository.save(exam);
        log.info("Created medical examination {} for {}", exam.getExaminationNumber(), student.getStudentId());

        return exam;
    }

    private String generateExamNumber() {
        int year = LocalDateTime.now().getYear();
        long count = medicalRepository.count() + 1;
        return String.format("MED-%d-%05d", year, count);
    }

    private Map<String, Object> convertToMap(MedicalExamination exam) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", exam.getId());
        map.put("examinationNumber", exam.getExaminationNumber());
        map.put("studentId", exam.getStudent().getStudentId());
        map.put("studentName", exam.getStudent().getFullName());
        map.put("status", exam.getStatus());
        map.put("progressPercentage", exam.getProgressPercentage());
        map.put("appointmentDate", exam.getAppointmentDate());
        map.put("clinicName", exam.getClinicName());
        map.put("clinicAddress", exam.getClinicAddress());
        map.put("clinicPhone", exam.getClinicPhone());
        map.put("examinationDate", exam.getExaminationDate());
        map.put("resultDate", exam.getResultDate());
        map.put("passed", exam.getPassed());
        map.put("resultNotes", exam.getResultNotes());
        map.put("chestXrayDone", exam.getChestXrayDone());
        map.put("bloodTestDone", exam.getBloodTestDone());
        map.put("urineTestDone", exam.getUrineTestDone());
        map.put("submittedToEmgs", exam.getSubmittedToEmgs());
        map.put("emgsSubmissionDate", exam.getEmgsSubmissionDate());
        map.put("emgsReference", exam.getEmgsReference());
        map.put("expiryDate", exam.getExpiryDate());
        map.put("createdAt", exam.getCreatedAt());
        map.put("updatedAt", exam.getUpdatedAt());
        return map;
    }

    // Email notifications

    private void sendAppointmentEmail(User student, MedicalExamination exam) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String subject = "Medical Appointment Confirmed - Taylor's E-Services";
        String message = String.format(
                "Dear %s,\n\n" +
                        "Your medical examination appointment has been confirmed:\n\n" +
                        "Date & Time: %s\n" +
                        "Clinic: %s\n" +
                        "Address: %s\n\n" +
                        "Please bring:\n" +
                        "- Passport\n" +
                        "- Student ID\n" +
                        "- Offer letter\n\n" +
                        "Best regards,\n" +
                        "Taylor's International Office",
                student.getFullName(),
                exam.getAppointmentDate().format(formatter),
                exam.getClinicName(),
                exam.getClinicAddress()
        );
        emailService.sendEmail(student.getEmail(), subject, message);
    }

    private void sendPassedEmail(User student, MedicalExamination exam) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String subject = "Medical Examination Passed - Congratulations!";
        String message = String.format(
                "Dear %s,\n\n" +
                        "Congratulations! You have passed your medical examination.\n\n" +
                        "Examination Number: %s\n" +
                        "Valid Until: %s\n\n" +
                        "Your medical clearance will be submitted to EMGS.\n\n" +
                        "Best regards,\n" +
                        "Taylor's International Office",
                student.getFullName(),
                exam.getExaminationNumber(),
                exam.getExpiryDate().format(formatter)
        );
        emailService.sendEmail(student.getEmail(), subject, message);
    }

    private void sendFailedEmail(User student, MedicalExamination exam) {
        String subject = "Medical Examination - Retest Required";
        String message = String.format(
                "Dear %s,\n\n" +
                        "Your medical examination requires further attention.\n\n" +
                        "Notes: %s\n\n" +
                        "Please contact the International Office for next steps.\n\n" +
                        "Best regards,\n" +
                        "Taylor's International Office",
                student.getFullName(),
                exam.getResultNotes()
        );
        emailService.sendEmail(student.getEmail(), subject, message);
    }

    private void sendEmgsSubmissionEmail(User student, MedicalExamination exam) {
        String subject = "Medical Report Submitted to EMGS";
        String message = String.format(
                "Dear %s,\n\n" +
                        "Your medical report has been submitted to EMGS.\n\n" +
                        "EMGS Reference: %s\n" +
                        "Submission Date: %s\n\n" +
                        "You will be notified once EMGS approval is received.\n\n" +
                        "Best regards,\n" +
                        "Taylor's International Office",
                student.getFullName(),
                exam.getEmgsReference(),
                exam.getEmgsSubmissionDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        );
        emailService.sendEmail(student.getEmail(), subject, message);
    }
}
