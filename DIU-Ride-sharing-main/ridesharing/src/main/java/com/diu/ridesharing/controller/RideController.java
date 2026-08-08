package com.diu.ridesharing.controller;

import com.diu.ridesharing.dto.*;
import com.diu.ridesharing.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @PostMapping("/offer")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponseDTO> offerRide(@RequestBody @Valid OfferRideDTO dto) {
        return ResponseEntity.ok(rideService.offerRide(dto));
    }

    @GetMapping("/offers")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<RideResponseDTO>> listOffers(
            @RequestParam(required = false) String pickup,
            @RequestParam(required = false) String dropoff) {
        return ResponseEntity.ok(rideService.getAvailableOffers(pickup, dropoff));
    }

    @PutMapping("/{rideId}/book")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<RideResponseDTO> bookRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.bookRide(rideId, null));
    }

    @PutMapping("/{rideId}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponseDTO> startRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.startRide(rideId));
    }

    @PutMapping("/{rideId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponseDTO> completeRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.completeRide(rideId));
    }

    @PutMapping("/{rideId}/cancel")
    @PreAuthorize("hasAnyRole('DRIVER','RIDER')")
    public ResponseEntity<RideResponseDTO> cancelRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.cancelRide(rideId));
    }

    @PutMapping("/cancel/self")
    @PreAuthorize("hasAnyRole('DRIVER','RIDER')")
    public ResponseEntity<RideResponseDTO> cancelSelf() {
        return ResponseEntity.ok(rideService.cancelSelf());
    }

    @PutMapping("/rider/force-complete")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<RideResponseDTO> riderForceComplete() {
        return ResponseEntity.ok(rideService.riderForceComplete());
    }

    @GetMapping("/driver/dashboard")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverDashboardDTO> driverDashboard() {
        return ResponseEntity.ok(rideService.getDriverDashboard());
    }

    @GetMapping("/driver/offers")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<RideResponseDTO>> myOfferedRidesForDriver() {
        return ResponseEntity.ok(rideService.getMyOfferedRidesForDriver());
    }

    @GetMapping("/mine/latest")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<RideResponseDTO> myLatest() {
        return ResponseEntity.ok(rideService.getRiderLatest());
    }

    //NEW: Driver History ===
    @GetMapping("/driver/history")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<RideResponseDTO>> driverHistory() {
        return ResponseEntity.ok(rideService.getDriverHistory());
    }

    //NEW: Rider History ===
    @GetMapping("/rider/history")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<RideResponseDTO>> riderHistory() {
        return ResponseEntity.ok(rideService.getRiderHistory());
    }
}
