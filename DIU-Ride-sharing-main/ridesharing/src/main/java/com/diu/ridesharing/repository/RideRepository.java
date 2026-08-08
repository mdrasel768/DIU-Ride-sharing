package com.diu.ridesharing.repository;

import com.diu.ridesharing.entity.Ride;
import com.diu.ridesharing.entity.Ride.RideStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByStatus(RideStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r where r.id = :id")
    Optional<Ride> findWithLockingById(@Param("id") Long id);

    List<Ride> findByDriverIdAndStatus(Long driverId, RideStatus status);
    boolean existsByDriverIdAndStatus(Long driverId, RideStatus status);

    boolean existsByRiderIdAndStatusIn(Long riderId, List<RideStatus> statuses);

    Optional<Ride> findTopByRiderIdOrderByRequestTimeDesc(Long riderId);
    Optional<Ride> findTopByRiderIdAndStatusOrderByRequestTimeDesc(Long riderId, RideStatus status);
    Optional<Ride> findTopByDriverIdAndStatusOrderByRequestTimeDesc(Long driverId, RideStatus status);

    Optional<Ride> findTopByRiderIdOrDriverIdAndStatusInOrderByRequestTimeDesc(
            Long riderId, Long driverId, List<RideStatus> statuses
    );

    List<Ride> findByDriverIdAndStatusIn(Long driverId, List<RideStatus> statuses);

    List<Ride> findByStatusAndRequestTimeBefore(RideStatus status, LocalDateTime cutoffTime);

    List<Ride> findTop10ByDriverIdAndStatusInOrderByRequestTimeDesc(Long driverId, List<RideStatus> statuses);
    List<Ride> findTop10ByRiderIdAndStatusInOrderByRequestTimeDesc(Long riderId, List<RideStatus> statuses);
}
