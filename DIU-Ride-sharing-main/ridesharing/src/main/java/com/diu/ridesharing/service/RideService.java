package com.diu.ridesharing.service;

import com.diu.ridesharing.dto.ConfirmRideDTO;
import com.diu.ridesharing.dto.DriverDashboardDTO;
import com.diu.ridesharing.dto.OfferRideDTO;
import com.diu.ridesharing.dto.RideResponseDTO;
import com.diu.ridesharing.entity.Driver;
import com.diu.ridesharing.entity.Ride;
import com.diu.ridesharing.entity.Rider;
import com.diu.ridesharing.entity.User;
import com.diu.ridesharing.exception.ResourceNotFoundException;
import com.diu.ridesharing.repository.RideRepository;
import com.diu.ridesharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    // ===== Fare Quote =====
    public int quoteFare(String pickup, String dropoff) {
        if (pickup == null || dropoff == null) throw new IllegalArgumentException("Pickup and dropoff are required.");
        if (pickup.equalsIgnoreCase(dropoff)) throw new IllegalArgumentException("Pickup and dropoff cannot be the same.");
        boolean hasDSC = "DSC".equalsIgnoreCase(pickup) || "DSC".equalsIgnoreCase(dropoff);
        if (!hasDSC) throw new IllegalArgumentException("Either pickup or dropoff must be DSC.");
        String other = "DSC".equalsIgnoreCase(pickup) ? dropoff : pickup;

        switch (other.toLowerCase()) {
            case "uttara":  return 150;
            case "banani":  return 200;
            case "gulshan": return 220;
            case "mirpur":  return 140;
            default:        return 200;
        }
    }

    // ===== DRIVER: create offer =====
    @Transactional
    public RideResponseDTO offerRide(OfferRideDTO dto) {
        if (dto.getPickupLocation().equalsIgnoreCase(dto.getDropoffLocation())) {
            throw new IllegalArgumentException("Pickup and dropoff cannot be the same.");
        }
        boolean hasDSC = "DSC".equalsIgnoreCase(dto.getPickupLocation()) || "DSC".equalsIgnoreCase(dto.getDropoffLocation());
        if (!hasDSC) throw new IllegalArgumentException("Either pickup or dropoff must be DSC.");

        Driver driver = getAuthenticatedDriver();
        double fare = dto.getFare() != null ? dto.getFare() : quoteFare(dto.getPickupLocation(), dto.getDropoffLocation());

        Ride ride = new Ride();
        ride.setPickupLocation(dto.getPickupLocation());
        ride.setDropoffLocation(dto.getDropoffLocation());
        ride.setRequestTime(LocalDateTime.now());
        ride.setFare(fare);
        ride.setDriver(driver);
        ride.setStatus(Ride.RideStatus.OFFERED);
        ride.setLastActionBy("DRIVER");

        return new RideResponseDTO(rideRepository.save(ride));
    }

    // ===== RIDER: list offers =====
    @Transactional(readOnly = true)
    public List<RideResponseDTO> getAvailableOffers(String pickup, String dropoff) {
        List<Ride> base = rideRepository.findByStatus(Ride.RideStatus.OFFERED);
        boolean hasPickup = pickup != null && !pickup.trim().isEmpty();
        boolean hasDrop   = dropoff != null && !dropoff.trim().isEmpty();

        return base.stream()
                .filter(r -> !hasPickup || pickup.equalsIgnoreCase(r.getPickupLocation()))
                .filter(r -> !hasDrop   || dropoff.equalsIgnoreCase(r.getDropoffLocation()))
                .map(RideResponseDTO::new)
                .collect(Collectors.toList());
    }

    // ===== RIDER: book =====
    @Transactional
    public RideResponseDTO bookRide(Long rideId, ConfirmRideDTO _ignored) {
        User user = getAuthenticatedUser();
        if (user.getRole() != User.Role.RIDER) throw new SecurityException("Only riders can confirm.");
        Rider rider = (user instanceof Rider) ? (Rider) user : null;
        if (rider == null) throw new IllegalStateException("Rider entity not resolvable.");

        List<Ride.RideStatus> active = Arrays.asList(Ride.RideStatus.BOOKED, Ride.RideStatus.IN_PROGRESS);
        if (rideRepository.existsByRiderIdAndStatusIn(rider.getId(), active)) {
            throw new IllegalStateException("You already have an active ride. Complete or cancel it first.");
        }

        Ride ride = rideRepository.findWithLockingById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));
        if (ride.getStatus() != Ride.RideStatus.OFFERED) {
            throw new IllegalStateException("Ride is not available for booking");
        }

        ride.setRider(rider);
        ride.setStatus(Ride.RideStatus.BOOKED);
        ride.setLastActionBy("RIDER");
        return new RideResponseDTO(rideRepository.save(ride));
    }

    // ===== DRIVER: start =====
    @Transactional
    public RideResponseDTO startRide(Long rideId) {
        Driver driver = getAuthenticatedDriver();
        Ride ride = rideRepository.findWithLockingById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));

        if (!Objects.equals(ride.getDriver().getId(), driver.getId()))
            throw new SecurityException("Only the offering driver can start this ride");
        if (ride.getStatus() != Ride.RideStatus.BOOKED)
            throw new IllegalStateException("Ride must be BOOKED to start");

        ride.setStatus(Ride.RideStatus.IN_PROGRESS);
        ride.setStartTime(LocalDateTime.now());
        ride.setLastActionBy("DRIVER");
        Ride saved = rideRepository.save(ride);

        // Cancel other offers by same driver
        List<Ride> others = rideRepository.findByDriverIdAndStatus(driver.getId(), Ride.RideStatus.OFFERED);
        LocalDateTime now = LocalDateTime.now();
        for (Ride o : others) {
            if (!Objects.equals(o.getId(), saved.getId())) {
                o.setStatus(Ride.RideStatus.CANCELLED);
                o.setEndTime(now);
                o.setLastActionBy("SYSTEM_AUTOCANCEL");
                rideRepository.save(o);
            }
        }
        return new RideResponseDTO(saved);
    }

    // ===== DRIVER: complete =====
    @Transactional
    public RideResponseDTO completeRide(Long rideId) {
        Driver driver = getAuthenticatedDriver();
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));

        if (!Objects.equals(ride.getDriver().getId(), driver.getId()))
            throw new SecurityException("Only the assigned driver can complete the ride");
        if (ride.getStatus() != Ride.RideStatus.IN_PROGRESS)
            throw new IllegalStateException("Ride is not in progress");

        ride.setStatus(Ride.RideStatus.COMPLETED);
        ride.setEndTime(LocalDateTime.now());
        ride.setLastActionBy("DRIVER");
        return new RideResponseDTO(rideRepository.save(ride));
    }

    // ===== BOTH: cancel by id =====
    @Transactional
    public RideResponseDTO cancelRide(Long rideId) {
        User current = getAuthenticatedUser();
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));

        if (ride.getStatus() == Ride.RideStatus.COMPLETED)
            throw new IllegalStateException("Completed ride cannot be cancelled.");

        Long currentId = current.getId();
        Long riderId   = ride.getRider()  != null ? ride.getRider().getId()  : null;
        Long driverId  = ride.getDriver() != null ? ride.getDriver().getId() : null;

        boolean isRider  = Objects.equals(riderId, currentId);
        boolean isDriver = Objects.equals(driverId, currentId);
        if (!(isRider || isDriver)) throw new SecurityException("Not authorized to cancel");

        if (isRider && ride.getStatus() == Ride.RideStatus.IN_PROGRESS) {
            throw new IllegalStateException("Driver has started the ride. Rider cannot cancel now.");
        }

        if (isRider && ride.getStatus() == Ride.RideStatus.BOOKED) {
            ride.setStatus(Ride.RideStatus.OFFERED);
            ride.setRider(null);
            ride.setLastActionBy("RIDER_CANCEL_REOPEN");
            return new RideResponseDTO(rideRepository.save(ride));
        }

        ride.setStatus(Ride.RideStatus.CANCELLED);
        ride.setEndTime(LocalDateTime.now());
        ride.setLastActionBy(isDriver ? "DRIVER" : "RIDER");
        return new RideResponseDTO(rideRepository.save(ride));
    }

    // ===== BOTH: cancel latest =====
    @Transactional
    public RideResponseDTO cancelSelf() {
        User current = getAuthenticatedUser();
        List<Ride.RideStatus> active = Arrays.asList(Ride.RideStatus.BOOKED, Ride.RideStatus.IN_PROGRESS);
        Ride target = rideRepository
                .findTopByRiderIdOrDriverIdAndStatusInOrderByRequestTimeDesc(current.getId(), current.getId(), active)
                .orElseThrow(() -> new IllegalStateException("No active ride to cancel"));

        if (current.getRole() == User.Role.RIDER && target.getStatus() == Ride.RideStatus.IN_PROGRESS) {
            throw new IllegalStateException("Driver has started the ride. Rider cannot cancel now.");
        }

        if (current.getRole() == User.Role.RIDER && target.getStatus() == Ride.RideStatus.BOOKED) {
            target.setStatus(Ride.RideStatus.OFFERED);
            target.setRider(null);
            target.setLastActionBy("RIDER_CANCEL_REOPEN");
            return new RideResponseDTO(rideRepository.save(target));
        }

        target.setStatus(Ride.RideStatus.CANCELLED);
        target.setEndTime(LocalDateTime.now());
        target.setLastActionBy(current.getRole().name());
        return new RideResponseDTO(rideRepository.save(target));
    }

    // ===== RIDER: force-complete =====
    @Transactional
    public RideResponseDTO riderForceComplete() {
        User user = getAuthenticatedUser();
        if (user.getRole() != User.Role.RIDER) throw new SecurityException("Only rider can force-complete");

        Ride ride = rideRepository
                .findTopByRiderIdAndStatusOrderByRequestTimeDesc(user.getId(), Ride.RideStatus.IN_PROGRESS)
                .orElseThrow(() -> new IllegalStateException("No in-progress ride found"));

        if (ride.getStartTime() == null) throw new IllegalStateException("Ride not started.");
        long mins = Duration.between(ride.getStartTime(), LocalDateTime.now()).toMinutes();
        if (mins < 30) throw new IllegalStateException("Force complete allowed 30 minutes after start.");

        ride.setStatus(Ride.RideStatus.COMPLETED);
        ride.setEndTime(LocalDateTime.now());
        ride.setLastActionBy("RIDER_FORCE");
        return new RideResponseDTO(rideRepository.save(ride));
    }

    // ===== DRIVER dashboard =====
    @Transactional(readOnly = true)
    public DriverDashboardDTO getDriverDashboard() {
        Driver driver = getAuthenticatedDriver();

        List<RideResponseDTO> pending = rideRepository
                .findByDriverIdAndStatus(driver.getId(), Ride.RideStatus.BOOKED)
                .stream().map(RideResponseDTO::new).collect(Collectors.toList());

        RideResponseDTO current = rideRepository
                .findTopByDriverIdAndStatusOrderByRequestTimeDesc(driver.getId(), Ride.RideStatus.IN_PROGRESS)
                .map(RideResponseDTO::new).orElse(null);

        return new DriverDashboardDTO(pending, current);
    }

    // ===== DRIVER: offered =====
    @Transactional(readOnly = true)
    public List<RideResponseDTO> getMyOfferedRidesForDriver() {
        Driver driver = getAuthenticatedDriver();
        return rideRepository.findByDriverIdAndStatus(driver.getId(), Ride.RideStatus.OFFERED)
                .stream().map(RideResponseDTO::new).collect(Collectors.toList());
    }

    // ===== RIDER: latest =====
    @Transactional(readOnly = true)
    public RideResponseDTO getRiderLatest() {
        User user = getAuthenticatedUser();
        if (user.getRole() != User.Role.RIDER) throw new SecurityException("Only riders can access this");
        return rideRepository.findTopByRiderIdOrderByRequestTimeDesc(user.getId())
                .map(RideResponseDTO::new).orElse(null);
    }

    // ===== HISTORY: driver =====
    @Transactional(readOnly = true)
    public List<RideResponseDTO> getDriverHistory() {
        Driver driver = getAuthenticatedDriver();
        List<Ride.RideStatus> past = Arrays.asList(Ride.RideStatus.COMPLETED, Ride.RideStatus.CANCELLED);
        return rideRepository.findTop10ByDriverIdAndStatusInOrderByRequestTimeDesc(driver.getId(), past)
                .stream().map(RideResponseDTO::new).collect(Collectors.toList());
    }

    // ===== HISTORY: rider =====
    @Transactional(readOnly = true)
    public List<RideResponseDTO> getRiderHistory() {
        User user = getAuthenticatedUser();
        List<Ride.RideStatus> past = Arrays.asList(Ride.RideStatus.COMPLETED, Ride.RideStatus.CANCELLED);
        return rideRepository.findTop10ByRiderIdAndStatusInOrderByRequestTimeDesc(user.getId(), past)
                .stream().map(RideResponseDTO::new).collect(Collectors.toList());
    }

    // ===== AUTO-CANCEL scheduler =====
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCancelOldOffers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        List<Ride> oldOffers = rideRepository.findByStatusAndRequestTimeBefore(Ride.RideStatus.OFFERED, cutoff);
        for (Ride r : oldOffers) {
            r.setStatus(Ride.RideStatus.CANCELLED);
            r.setEndTime(LocalDateTime.now());
            r.setLastActionBy("SYSTEM_AUTOCANCEL_1HR");
            rideRepository.save(r);
        }
    }

    // ===== helpers =====
    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Driver getAuthenticatedDriver() {
        User user = getAuthenticatedUser();
        if (user.getRole() != User.Role.DRIVER) throw new SecurityException("Only drivers can perform this action");
        if (user instanceof Driver) return (Driver) user;
        throw new IllegalStateException("Driver entity not resolvable");
    }
}