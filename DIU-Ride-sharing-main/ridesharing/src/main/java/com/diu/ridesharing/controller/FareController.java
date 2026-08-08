// src/main/java/com/diu/ridesharing/controller/FareController.java
package com.diu.ridesharing.controller;

import com.diu.ridesharing.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fares")
@RequiredArgsConstructor
public class FareController {

    private final RideService rideService;

    @GetMapping("/quote")
    public ResponseEntity<Integer> quote(@RequestParam String pickup, @RequestParam String dropoff) {
        int fare = rideService.quoteFare(pickup, dropoff);
        return ResponseEntity.ok(fare);
    }
}
