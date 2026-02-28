package edu.taylors.io.capstone.eservices.controller;

import edu.taylors.io.capstone.eservices.dto.FileListResponse;
import edu.taylors.io.capstone.eservices.dto.FileUploadResponse;
import edu.taylors.io.capstone.eservices.entity.FileCategory;
import edu.taylors.io.capstone.eservices.entity.StudentFile;
import edu.taylors.io.capstone.eservices.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/student/files")
@RequiredArgsConstructor
public class StudentFileController {

    private final FileStorageService fileStorageService;

    /**
     * Upload a new file
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") FileCategory category,
            @RequestParam(value = "description", required = false) String description) {
        try {
            String studentId = authentication.getName();
            FileUploadResponse response = fileStorageService.uploadFile(studentId, file, category, description);

            return ResponseEntity.ok(Map.of(
                    "message", "File uploaded successfully",
                    "file", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get all files for the authenticated student
     * Optional: filter by category
     */
    @GetMapping
    public ResponseEntity<FileListResponse> getFiles(
            Authentication authentication,
            @RequestParam(value = "category", required = false) FileCategory category) {
        String studentId = authentication.getName();
        FileListResponse response = fileStorageService.getStudentFiles(studentId, category);
        return ResponseEntity.ok(response);
    }

    /**
     * Get file categories
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(Map.of(
                "categories", FileCategory.values()
        ));
    }

    /**
     * Download a specific file
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            Authentication authentication,
            @PathVariable Long id) {
        try {
            String studentId = authentication.getName();

            // Get file metadata
            StudentFile fileMetadata = fileStorageService.getFileMetadata(studentId, id);

            // Get file resource
            Resource resource = fileStorageService.downloadFile(studentId, id);

            // Set content type
            String contentType = fileMetadata.getFileType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileMetadata.getOriginalFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get file metadata/details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getFileDetails(
            Authentication authentication,
            @PathVariable Long id) {
        try {
            String studentId = authentication.getName();
            StudentFile file = fileStorageService.getFileMetadata(studentId, id);

            return ResponseEntity.ok(Map.of(
                    "id", file.getId(),
                    "fileName", file.getOriginalFileName(),
                    "fileType", file.getFileType(),
                    "fileSize", file.getFormattedFileSize(),
                    "category", file.getCategory(),
                    "description", file.getDescription() != null ? file.getDescription() : "",
                    "uploadedAt", file.getUploadedAt(),
                    "downloadUrl", "/api/student/files/" + file.getId() + "/download"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Delete a file
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(
            Authentication authentication,
            @PathVariable Long id) {
        try {
            String studentId = authentication.getName();
            fileStorageService.deleteFile(studentId, id);

            return ResponseEntity.ok(Map.of(
                    "message", "File deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}