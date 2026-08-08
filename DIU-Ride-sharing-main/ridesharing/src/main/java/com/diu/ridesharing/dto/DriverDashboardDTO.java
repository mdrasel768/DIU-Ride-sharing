
package com.diu.ridesharing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriverDashboardDTO {
    // BOOKED rides waiting for driver to start
    private List<RideResponseDTO> pending;
    // current IN_PROGRESS ride (if any)
    private RideResponseDTO current;
}
