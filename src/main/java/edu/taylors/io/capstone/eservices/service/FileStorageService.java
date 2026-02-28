package edu.taylors.io.capstone.eservices.service;

import edu.taylors.io.capstone.eservices.dto.FileListResponse;
import edu.taylors.io.capstone.eservices.dto.FileUploadResponse;
import edu.taylors.io.capstone.eservices.entity.FileCategory;
import edu.taylors.io.capstone.eservices.entity.StudentFile;
import edu.taylors.io.capstone.eservices.entity.User;
import edu.taylors.io.capstone.eservices.repository.StudentFileRepository;
import edu.taylors.io.capstone.eservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final StudentFileRepository fileRepository;
    private final UserRepository userRepository;

    @Value("${file.upload.dir:uploads/student-files}")
    private String uploadDir;

    @Value("${file.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;

    private static final List<String> ALLOWED_FILE_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Transactional
    public FileUploadResponse uploadFile(
            String studentId,
            MultipartFile file,
            FileCategory category,
            String description) throws IOException {

        // Validate file
        validateFile(file);

        // Get user
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, studentId);
        Files.createDirectories(uploadPath);

        // Generate unique filename
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String uniqueFileName = generateUniqueFileName(fileExtension);

        // Save file to disk
        Path targetLocation = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Save metadata to database
        StudentFile studentFile = StudentFile.builder()
                .user(user)
                .fileName(uniqueFileName)
                .originalFileName(originalFileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(targetLocation.toString())
                .category(category)
                .description(description)
                .build();

        studentFile = fileRepository.save(studentFile);

        // Return response
        return mapToFileUploadResponse(studentFile);
    }

    public FileListResponse getStudentFiles(String studentId, FileCategory category) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<StudentFile> files;
        if (category != null) {
            files = fileRepository.findByUserAndCategoryOrderByUploadedAtDesc(user, category);
        } else {
            files = fileRepository.findByUserOrderByUploadedAtDesc(user);
        }

        List<FileUploadResponse> fileResponses = files.stream()
                .map(this::mapToFileUploadResponse)
                .collect(Collectors.toList());

        long totalFiles = fileRepository.countByUser(user);
        Long totalStorageBytes = fileRepository.sumFileSizeByUser(user);
        if (totalStorageBytes == null) {
            totalStorageBytes = 0L;
        }

        return FileListResponse.builder()
                .files(fileResponses)
                .totalFiles(totalFiles)
                .totalStorageUsed(formatFileSize(totalStorageBytes))
                .totalStorageUsedBytes(totalStorageBytes)
                .build();
    }

    public Resource downloadFile(String studentId, Long fileId) throws IOException {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudentFile file = fileRepository.findByIdAndUser(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        Path filePath = Paths.get(file.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("File not found or not readable");
        }
    }

    public StudentFile getFileMetadata(String studentId, Long fileId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return fileRepository.findByIdAndUser(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));
    }

    @Transactional
    public void deleteFile(String studentId, Long fileId) throws IOException {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudentFile file = fileRepository.findByIdAndUser(fileId, user)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        // Delete physical file
        Path filePath = Paths.get(file.getFilePath());
        Files.deleteIfExists(filePath);

        // Delete database record
        fileRepository.delete(file);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Cannot upload empty file");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum limit of " + formatFileSize(maxFileSize));
        }

        String contentType = file.getContentType();
        if (!ALLOWED_FILE_TYPES.contains(contentType)) {
            throw new RuntimeException("File type not allowed. Allowed types: PDF, Images (JPG, PNG, GIF), Word, Excel");
        }
    }

    private String generateUniqueFileName(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + uuid + extension;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private FileUploadResponse mapToFileUploadResponse(StudentFile file) {
        return FileUploadResponse.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .originalFileName(file.getOriginalFileName())
                .fileType(file.getFileType())
                .fileSize(file.getFormattedFileSize())
                .fileSizeBytes(file.getFileSize())
                .category(file.getCategory())
                .description(file.getDescription())
                .uploadedAt(file.getUploadedAt())
                .downloadUrl("/api/student/files/" + file.getId() + "/download")
                .build();
    }
}