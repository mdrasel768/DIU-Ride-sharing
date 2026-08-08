package com.diu.ridesharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegistrationRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(01[3-9]\\d{8})$", message = "Invalid Bangladeshi phone number")
    private String phone;

    @NotBlank(message = "Email is required")
    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@diu\\.edu\\.bd$",
            message = "Only DIU email addresses (@diu.edu.bd) are allowed"
    )
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = ".{8,}",
            message = "Password must be at least 8 characters"
    )
    private String password;

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Department is required")
    private String department;

    private String licenseNumber;
    private String bikeNumber;
}