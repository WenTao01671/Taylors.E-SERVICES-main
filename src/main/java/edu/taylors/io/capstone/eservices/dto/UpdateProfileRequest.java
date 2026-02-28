package edu.taylors.io.capstone.eservices.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String email;
}