package com.diu.ridesharing.dto;

import com.diu.ridesharing.entity.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRowDTO {
    private long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String status;
    private String studentId;
    private String department;

    public static UserRowDTO from(User u) {
        if (u == null) return null;
        return UserRowDTO.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole() != null ? u.getRole().name() : null)
                .status(u.getStatus())
                .studentId(u.getStudentId())
                .department(u.getDepartment())
                .build();
    }
}