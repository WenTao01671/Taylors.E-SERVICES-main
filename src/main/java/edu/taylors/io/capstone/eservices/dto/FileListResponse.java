package edu.taylors.io.capstone.eservices.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileListResponse {
    private List<FileUploadResponse> files;
    private long totalFiles;
    private String totalStorageUsed;
    private long totalStorageUsedBytes;
}