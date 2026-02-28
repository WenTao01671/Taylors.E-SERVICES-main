package edu.taylors.io.capstone.eservices.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {
    private String studentId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String profilePictureUrl;
    private String role;
    private boolean twoFactorEnabled;
    private String twoFactorMethod;
}