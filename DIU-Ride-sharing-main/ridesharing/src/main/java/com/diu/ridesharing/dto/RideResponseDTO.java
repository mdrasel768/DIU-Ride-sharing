package com.diu.ridesharing.dto;

import com.diu.ridesharing.entity.Ride;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideResponseDTO {
    private Long id;
    private String pickupLocation;
    private String dropoffLocation;
    private double fare;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Long riderId;
    private Long driverId;

    private String driverPhone;
    private String riderPhone;
    private String lastActionBy;

    private String driverName;
    private String driverStudentId;
    private String driverEmail;
    private String driverBikeNumber;

    private String riderName;
    private String riderStudentId;
    private String riderEmail;

    public RideResponseDTO(Ride r) {
        this.id = r.getId();
        this.pickupLocation = r.getPickupLocation();
        this.dropoffLocation = r.getDropoffLocation();
        this.fare = r.getFare();
        this.status = r.getStatus() != null ? r.getStatus().name() : null;
        this.requestTime = r.getRequestTime();
        this.startTime = r.getStartTime();
        this.endTime = r.getEndTime();

        this.riderId = r.getRider() != null ? r.getRider().getId() : null;
        this.driverId = r.getDriver() != null ? r.getDriver().getId() : null;

        this.lastActionBy = r.getLastActionBy();

        if (r.getDriver() != null) {
            this.driverName = r.getDriver().getName();
            this.driverStudentId = r.getDriver().getStudentId();
            this.driverEmail = r.getDriver().getEmail();
            this.driverPhone = r.getDriver().getPhone();
            this.driverBikeNumber = r.getDriver().getBikeNumber();
        }

        if (r.getRider() != null) {
            this.riderName = r.getRider().getName();
            this.riderStudentId = r.getRider().getStudentId();
            this.riderEmail = r.getRider().getEmail();
            this.riderPhone = r.getRider().getPhone();
        }
    }
}