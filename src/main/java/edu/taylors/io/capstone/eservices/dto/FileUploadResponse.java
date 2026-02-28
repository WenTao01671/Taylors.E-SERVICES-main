package edu.taylors.io.capstone.eservices.dto;

import edu.taylors.io.capstone.eservices.entity.FileCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private String fileSize;
    private Long fileSizeBytes;
    private FileCategory category;
    private String description;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
}