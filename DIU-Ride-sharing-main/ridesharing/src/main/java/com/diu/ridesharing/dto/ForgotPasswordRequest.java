package com.diu.ridesharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@diu\\.edu\\.bd$",
            message = "Only DIU email addresses (@diu.edu.bd) are allowed"
    )
    private String email;
}
