
package com.diu.ridesharing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OfferRideDTO {
    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;

    @NotBlank(message = "Dropoff location is required")
    private String dropoffLocation;

    private Double fare;   // optional
}
