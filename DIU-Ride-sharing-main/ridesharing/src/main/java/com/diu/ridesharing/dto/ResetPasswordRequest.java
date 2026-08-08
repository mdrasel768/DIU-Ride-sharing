package com.diu.ridesharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Pattern(
            regexp = ".{8,}",
            message = "Password must be at least 8 characters"
    )
    private String newPassword;
}
