package edu.taylors.io.capstone.eservices.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Email or Student ID is required")
    private String emailOrStudentId;
}