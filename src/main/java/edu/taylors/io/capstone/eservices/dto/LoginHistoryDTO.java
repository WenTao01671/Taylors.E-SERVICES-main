package edu.taylors.io.capstone.eservices.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistoryDTO {
    private Long id;
    private String ipAddress;
    private String userAgent;
    private String location;
    private LocalDateTime loginTime;
    private boolean successful;
}