package com.example.booking.repository;

import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    @Query("""
        SELECT COUNT(r) > 0 FROM Reservation r
        WHERE r.resource.id = :resourceId
          AND r.status IN :statuses
          AND r.startTime < :endTime
          AND r.endTime > :startTime
          AND (:excludeId IS NULL OR r.id != :excludeId)
    """)
    boolean existsOverlappingReservation(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("excludeId") Long excludeId);
}
