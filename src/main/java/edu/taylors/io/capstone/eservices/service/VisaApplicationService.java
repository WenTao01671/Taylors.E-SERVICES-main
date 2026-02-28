package edu.taylors.io.capstone.eservices.service;

import edu.taylors.io.capstone.eservices.entity.MedicalExamination;
import edu.taylors.io.capstone.eservices.entity.User;
import edu.taylors.io.capstone.eservices.entity.VisaApplication;
import edu.taylors.io.capstone.eservices.repository.MedicalExaminationRepository;
import edu.taylors.io.capstone.eservices.repository.UserRepository;
import edu.taylors.io.capstone.eservices.repository.VisaApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisaApplicationService {

    private final VisaApplicationRepository visaRepository;
    private final UserRepository userRepository;
    private final MedicalExaminationRepository medicalRepository;
    private final MedicalExaminationService medicalService;
    private final EmailService emailService;

    /**
     * Get or create visa application for student
     */
    @Transactional
    public Map<String, Object> getVisaApplication(String studentId) {
        User student = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        VisaApplication visa = visaRepository.findByStudent(student)
                .orElseGet(() -> createNewVisaApplication(student));

        return convertToMap(visa);
    }

    /**
     * Create new visa application with program details
     */
    @Transactional
    public Map<String, Object> createVisaApplication(String studentId, Map<String, Object> request) {
        User student = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if already exists
        if (visaRepository.findByStudent(student).isPresent()) {
            throw new RuntimeException("Visa application already exists");
        }

        VisaApplication visa = VisaApplication.builder()
                .student(student)
                .applicationNumber(generateApplicationNumber())
                .visaType((String) request.getOrDefault("visaType", "STUDENT_PASS"))
                .passportNumber((String) request.get("passportNumber"))
                .passportExpiry(parseDate((String) request.get("passportExpiry")))
                .nationality((String) request.get("nationality"))
                .programName((String) request.get("programName"))
                .faculty((String) request.get("faculty"))
                .programStartDate(parseDate((String) request.get("programStartDate")))
                .status("PENDING")
                .currentStage("Application Created")
                .applicationDate(LocalDateTime.now())
                .build();

        visa.updateProgress();
        visa = visaRepository.save(visa);

        // Also create medical examination if doesn't exist
        medicalService.getMedicalExamination(studentId);

        log.info("Created visa application {} for {}", visa.getApplicationNumber(), studentId);

        sendApplicationCreatedEmail(student, visa);

        return Map.of(
                "message", "Visa application created successfully",
                "application", convertToMap(visa)
        );
    }

    /**
     * Submit documents
     */
    @Transactional
    public Map<String, Object> submitDocuments(String studentId) {
        User student = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        VisaApplication visa = visaRepository.findByStudent(student)
                .orElseThrow(() -> new RuntimeException("Visa application not found"));

        visa.setDocumentsSubmittedDate(LocalDateTime.now());
        visa.setStatus("DOCUMENTS_SUBMITTED");
        visa.setCurrentStage("Documents Submitted - Pending Review");
        visa.updateProgress();

        visaRepository.save(visa);

        sendDocumentsSubmittedEmail(student, visa);

        return Map.of(
                "message", "Documents submitted successfully",
                "application", convertToMap(visa)
        );
    }

    /**
     * Submit to EMGS (Staff only)
     */
    @Transactional
    public Map<String, Object> submitToEmgs(Long id, String staffId) {
        VisaApplication visa = visaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Visa application not found"));

        // Check if ready
        if (!visa.isReadyForEmgsSubmission()) {
            throw new RuntimeException("Cannot submit to EMGS: Documents or medical not complete");
        }

        visa.setEmgsSubmissionDate(LocalDateTime.now());
        visa.setEmgsReference("EMGS-VISA-" + System.currentTimeMillis());
        visa.setStatus("EMGS_PROCESSING");
        visa.setCurrentStage("EMGS Processing - 0%");
        visa.setLastUpdatedBy(staffId);
        visa.updateProgress();

        visaRepository.save(visa);

        sendEmgsSubmissionEmail(visa.getStudent(), visa);

        return Map.of(
                "message", "Successfully submitted to EMGS",
                "emgsReference", visa.getEmgsReference(),
                "application", convertToMap(visa)
        );
    }

    /**
     * Update EMGS status (Staff only)
     */
    @Transactional
    public Map<String, Object> updateEmgsStatus(Long id, String newStatus, String notes, String staffId) {
        VisaApplication visa = visaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Visa application not found"));

        String oldStatus = visa.getStatus();
        visa.setStatus(newStatus);
        visa.setProcessingNotes(notes);
        visa.setLastUpdatedBy(staffId);

        // Update specific dates based on status
        switch (newStatus) {
            case "EMGS_APPROVED":
                visa.setEmgsApprovalDate(LocalDateTime.now());
                visa.setCurrentStage("EMGS Approved - 32%");
                break;
            case "VAL_ISSUED":
                visa.setValIssuedDate(LocalDateTime.now());
                visa.setValExpiryDate(LocalDateTime.now().plusMonths(6));  // VAL valid 6 months
                visa.setValNumber("VAL-" + System.currentTimeMillis());
                visa.setCurrentStage("VAL Issued - 70%");
                break;
            case "IMMIGRATION_SUBMITTED":
                visa.setImmigrationSubmissionDate(LocalDateTime.now());
                visa.setCurrentStage("Immigration Processing");
                break;
            case "IMMIGRATION_APPROVED":
                visa.setImmigrationApprovalDate(LocalDateTime.now());
                visa.setCurrentStage("Student Pass Approved - Ready for Collection");
                break;
            case "PASS_COLLECTED":
                visa.setPassCollectedDate(LocalDateTime.now());
                visa.setCurrentStage("Student Pass Collected - Complete");
                break;
            case "REJECTED":
                visa.setCurrentStage("Application Rejected");
                break;
        }

        visa.updateProgress();
        visaRepository.save(visa);

        sendStatusUpdateEmail(visa.getStudent(), visa, oldStatus, newStatus);

        return Map.of(
                "message", "Status updated successfully",
                "application", convertToMap(visa)
        );
    }

    /**
     * Get visa timeline (Student)
     */
    public Map<String, Object> getVisaTimeline(String studentId) {
        User student = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        VisaApplication visa = visaRepository.findByStudent(student)
                .orElseThrow(() -> new RuntimeException("Visa application not found"));

        List<Map<String, Object>> timeline = buildTimeline(visa);

        return Map.of(
                "applicationNumber", visa.getApplicationNumber(),
                "currentStatus", visa.getStatus(),
                "currentStage", visa.getCurrentStage(),
                "progressPercentage", visa.getProgressPercentage(),
                "timeline", timeline
        );
    }

    /**
     * Get all visa applications (Staff)
     */
    public List<Map<String, Object>> getAllVisaApplications(String status) {
        List<VisaApplication> applications;

        if (status != null && !status.isEmpty()) {
            applications = visaRepository.findByStatus(status);
        } else {
            applications = visaRepository.findAll();
        }

        return applications.stream()
                .map(this::convertToMap)
                .toList();
    }

    /**
     * Get visa statistics (Staff)
     */
    public Map<String, Object> getVisaStatistics() {
        long total = visaRepository.count();
        long pending = visaRepository.countByStatus("PENDING");
        long documentsSubmitted = visaRepository.countByStatus("DOCUMENTS_SUBMITTED");
        long emgsProcessing = visaRepository.countByStatus("EMGS_PROCESSING");
        long emgsApproved = visaRepository.countByStatus("EMGS_APPROVED");
        long valIssued = visaRepository.countByStatus("VAL_ISSUED");
        long immigrationProcessing = visaRepository.countByStatus("IMMIGRATION_SUBMITTED");
        long approved = visaRepository.countByStatus("IMMIGRATION_APPROVED");
        long collected = visaRepository.countByStatus("PASS_COLLECTED");
        long rejected = visaRepository.countByStatus("REJECTED");

        return Map.of(
                "totalApplications", total,
                "pending", pending,
                "documentsSubmitted", documentsSubmitted,
                "emgsProcessing", emgsProcessing,
                "emgsApproved", emgsApproved,
                "valIssued", valIssued,
                "immigrationProcessing", immigrationProcessing,
                "approved", approved,
                "collected", collected,
                "rejected", rejected
        );
    }

    // Helper methods

    private VisaApplication createNewVisaApplication(User student) {
        VisaApplication visa = VisaApplication.builder()
                .student(student)
                .applicationNumber(generateApplicationNumber())
                .status("PENDING")
                .currentStage("Not Started")
                .progressPercentage(0)
                .build();

        return visaRepository.save(visa);
    }

    private String generateApplicationNumber() {
        int year = LocalDateTime.now().getYear();
        long count = visaRepository.count() + 1;
        return String.format("VA-%d-%05d", year, count);
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateStr);
    }

    private List<Map<String, Object>> buildTimeline(VisaApplication visa) {
        List<Map<String, Object>> timeline = new java.util.ArrayList<>();

        if (visa.getApplicationDate() != null) {
            timeline.add(createTimelineEvent(
                    "Application Created",
                    visa.getApplicationDate(),
                    "completed",
                    "Application " + visa.getApplicationNumber() + " created"
            ));
        }

        if (visa.getDocumentsSubmittedDate() != null) {
            timeline.add(createTimelineEvent(
                    "Documents Submitted",
                    visa.getDocumentsSubmittedDate(),
                    "completed",
                    "All required documents submitted"
            ));
        }

        // Medical check
        if (visa.getMedicalExamination() != null) {
            MedicalExamination medical = visa.getMedicalExamination();
            String medicalStatus = medical.getStatus().equals("PASSED") ? "completed" : "in_progress";
            timeline.add(createTimelineEvent(
                    "Medical Examination",
                    medical.getResultDate(),
                    medicalStatus,
                    "Medical status: " + medical.getStatus()
            ));
        }

        if (visa.getEmgsSubmissionDate() != null) {
            timeline.add(createTimelineEvent(
                    "EMGS Submission",
                    visa.getEmgsSubmissionDate(),
                    "completed",
                    "Reference: " + visa.getEmgsReference()
            ));
        }

        if (visa.getEmgsApprovalDate() != null) {
            timeline.add(createTimelineEvent(
                    "EMGS Approved (32%)",
                    visa.getEmgsApprovalDate(),
                    "completed",
                    "EMGS approval received"
            ));
        }

        if (visa.getValIssuedDate() != null) {
            timeline.add(createTimelineEvent(
                    "VAL Issued (70%)",
                    visa.getValIssuedDate(),
                    "completed",
                    "VAL Number: " + visa.getValNumber()
            ));
        }

        if (visa.getImmigrationSubmissionDate() != null) {
            timeline.add(createTimelineEvent(
                    "Immigration Submission",
                    visa.getImmigrationSubmissionDate(),
                    "completed",
                    "Submitted to Immigration Department"
            ));
        }

        if (visa.getImmigrationApprovalDate() != null) {
            timeline.add(createTimelineEvent(
                    "Immigration Approved",
                    visa.getImmigrationApprovalDate(),
                    "completed",
                    "Student Pass approved"
            ));
        }

        if (visa.getPassCollectedDate() != null) {
            timeline.add(createTimelineEvent(
                    "Pass Collected",
                    visa.getPassCollectedDate(),
                    "completed",
                    "Student Pass collected - Process complete"
            ));
        }

        return timeline;
    }

    private Map<String, Object> createTimelineEvent(String title, LocalDateTime date, String status, String description) {
        Map<String, Object> event = new HashMap<>();
        event.put("title", title);
        event.put("date", date);
        event.put("status", status);
        event.put("description", description);
        return event;
    }

    private Map<String, Object> convertToMap(VisaApplication visa) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", visa.getId());
        map.put("applicationNumber", visa.getApplicationNumber());
        map.put("studentId", visa.getStudent().getStudentId());
        map.put("studentName", visa.getStudent().getFullName());
        map.put("status", visa.getStatus());
        map.put("currentStage", visa.getCurrentStage());
        map.put("progressPercentage", visa.getProgressPercentage());
        map.put("visaType", visa.getVisaType());
        map.put("applicationDate", visa.getApplicationDate());
        map.put("documentsSubmittedDate", visa.getDocumentsSubmittedDate());
        map.put("emgsSubmissionDate", visa.getEmgsSubmissionDate());
        map.put("emgsReference", visa.getEmgsReference());
        map.put("emgsApprovalDate", visa.getEmgsApprovalDate());
        map.put("valIssuedDate", visa.getValIssuedDate());
        map.put("valNumber", visa.getValNumber());
        map.put("valExpiryDate", visa.getValExpiryDate());
        map.put("passportNumber", visa.getPassportNumber());
        map.put("nationality", visa.getNationality());
        map.put("programName", visa.getProgramName());
        map.put("faculty", visa.getFaculty());
        map.put("medicalComplete", visa.isMedicalComplete());
        map.put("readyForEmgs", visa.isReadyForEmgsSubmission());
        map.put("createdAt", visa.getCreatedAt());
        map.put("updatedAt", visa.getUpdatedAt());
        return map;
    }

    // Email notifications

    private void sendApplicationCreatedEmail(User student, VisaApplication visa) {
        String subject = "Visa Application Created - Taylor's E-Services";
        String message = String.format(
                "Dear %s,\n\n" +
                        "Your visa application has been created successfully.\n\n" +
                        "Application Number: %s\n" +
                        "Visa Type: %s\n" +
                        "Program: %s\n\n" +
                        "Next Steps:\n" +
                        "1. Complete medical examination\n" +
                        "2. Submit required documents\n" +
                        "3. Wait for staff review\n\n" +
                        "You can track your application status in the E-Services portal.\n\n" +
                        "Best regards,\n" +
                        "Taylor's International Office",
                student.getFullName(),
                visa.getApplicationNumber(),
                visa.getVisaType(),
                visa.getProgramName()
        );
        emailService.sendEmail(student.getEmail(), subject, message);
    }

    private void sendDocumentsSubmittedEmail(User student, VisaApplication visa) {
        String subject = "Documents Submitted Successfully";
        String message = String.format(
                "Dear %s,\n\n" +
                        "Your documents have been submitted successfully.\n\n" +
                        "Application Number: %s\n" +
                        "Submission Date: %s\n\n" +
                        "Your application is now under review by the International Office staff.\n" +
                        "You will be notified once the review is complete.\n\n" +
                        "Best regards,\n" +
                        "Taylor's International Office",
                student.getFullName(),
                visa.getApplicationNumber(),
                visa.getDocumentsSubmittedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        );
        emailService.sendEmail(student.getEmail(), subject, message);
    }

    private void sendEmgsSubmissionEmail(User student, VisaApplication visa) {
        String subject = "Application Submitted to EMGS";
        String message = String.format(
                "Dear %s,\n\n" +
                        "Your visa application has been submitted to EMGS.\n\n" +
                        "Application Number: %s\n" +
                        "EMGS Reference: %s\n" +
                        "Submission Date: %s\n\n" +
                        "EMGS processing typically takes 10-14 working days.\n" +
                        "You will be notified once EMGS approval is received.\n\n" +
                        "Best regards,\n" +
                        "Taylor's International Office",
                student.getFullName(),
                visa.getApplicationNumber(),
                visa.getEmgsReference(),
                visa.getEmgsSubmissionDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        );
        emailService.sendEmail(student.getEmail(), subject, message);
    }

    private void sendStatusUpdateEmail(User student, VisaApplication visa, String oldStatus, String newStatus) {
        String subject = "Visa Application Status Update";
        String message = String.format(
                "Dear %s,\n\n" +
                        "Your visa application status has been updated.\n\n" +
                        "Application Number: %s\n" +
                        "Previous Status: %s\n" +
                        "Current Status: %s\n" +
                        "Current Stage: %s\n" +
                        "Progress: %d%%\n\n" +
                        "You can view full details in the E-Services portal.\n\n" +
                        "Best regards,\n" +
                        "Taylor's International Office",
                student.getFullName(),
                visa.getApplicationNumber(),
                oldStatus,
                newStatus,
                visa.getCurrentStage(),
                visa.getProgressPercentage()
        );
        emailService.sendEmail(student.getEmail(), subject, message);
    }
}
