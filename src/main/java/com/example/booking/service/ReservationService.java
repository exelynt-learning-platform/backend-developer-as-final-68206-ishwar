package com.example.booking.service;

import com.example.booking.dto.*;
import com.example.booking.entity.*;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ResourceRepository resourceRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request, String username) {
        User user = findUserByUsername(username);
        return createForUser(request.resourceId(), request.startTime(), request.endTime(), user);
    }

    @Transactional
    public ReservationResponse createForAdmin(AdminReservationRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.userId()));

        return createForUser(request.resourceId(), request.startTime(), request.endTime(), user);
    }

    private ReservationResponse createForUser(Long resourceId, LocalDateTime start,
                                              LocalDateTime end, User user) {
        validateTimes(start, end);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found"));

        if (!resource.isActive()) {
            throw new IllegalStateException("Resource is inactive");
        }

        if (isOverlapping(resource.getId(), start, end, null)) {
            throw new IllegalStateException("Resource is already reserved for the selected time");
        }

        Reservation reservation = new Reservation();
        reservation.setResource(resource);
        reservation.setUser(user);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setPrice(calculatePrice(resource, start, end));
        reservation.setStatus(ReservationStatus.PENDING);

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse updateOwn(Long id, ReservationUpdateRequest request,
                                         String username) {
        Reservation reservation = getOwnedReservation(id, username);
        updateReservation(reservation, request.resourceId(), request.startTime(), request.endTime());
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse updateAsAdmin(Long id, AdminReservationRequest request) {
        Reservation reservation = find(id);
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.userId()));

        updateReservation(reservation, request.resourceId(), request.startTime(), request.endTime());
        reservation.setUser(user);
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    private void updateReservation(Reservation reservation, Long resourceId,
                                   LocalDateTime start, LocalDateTime end) {
        validateTimes(start, end);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found"));

        if (!resource.isActive()) {
            throw new IllegalStateException("Resource is inactive");
        }

        if (isOverlapping(resource.getId(), start, end, reservation.getId())) {
            throw new IllegalStateException("Resource is already reserved for the selected time");
        }

        reservation.setResource(resource);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setPrice(calculatePrice(resource, start, end));
    }

    public Page<ReservationResponse> search(String username,
                                             boolean admin,
                                             ReservationStatus status,
                                             BigDecimal minPrice,
                                             BigDecimal maxPrice,
                                             int page,
                                             int size,
                                             String sortBy,
                                             String direction) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "page must be >= 0 and size must be between 1 and 100");
        }

        if (minPrice != null && minPrice.signum() < 0) {
            throw new IllegalArgumentException("minPrice must be greater than or equal to 0");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new IllegalArgumentException("maxPrice must be greater than or equal to 0");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }

        List<String> allowedSorts =
                List.of("id", "price", "startTime", "endTime", "status");

        if (sortBy == null || !allowedSorts.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sortBy. Allowed: " + allowedSorts);
        }

        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new IllegalArgumentException("direction must be either asc or desc");
        }

        Sort.Direction sortDirection =
                "desc".equalsIgnoreCase(direction)
                        ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable =
                PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!admin) {
                predicates.add(cb.equal(
                        root.get("user").get("username"), username));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return reservationRepository.findAll(spec, pageable)
                .map(ReservationResponse::from);
    }

    public ReservationResponse getByIdForCurrentUser(Long id,
                                                       String username,
                                                       boolean admin) {
        Reservation reservation = find(id);

        if (!admin && !reservation.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "You can access only your own reservation");
        }

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationStatus status) {
        Reservation reservation = find(id);
        reservation.setStatus(status);
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public void cancelOwn(Long id, String username) {
        Reservation reservation = getOwnedReservation(id, username);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void delete(Long id) {
        reservationRepository.delete(find(id));
    }

    private Reservation getOwnedReservation(Long id, String username) {
        Reservation reservation = find(id);

        if (!reservation.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "You can modify only your own reservation");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cancelled reservation cannot be modified");
        }

        return reservation;
    }

    private Reservation find(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Reservation not found: " + id));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private boolean isOverlapping(Long resourceId, LocalDateTime start,
                                  LocalDateTime end, Long excludeId) {
        List<ReservationStatus> statuses =
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

        return reservationRepository.existsOverlappingReservation(
                resourceId, start, end, statuses, excludeId);
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "endTime must be after startTime");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "startTime must be in the future");
        }
    }

    private BigDecimal calculatePrice(Resource resource,
                                      LocalDateTime start,
                                      LocalDateTime end) {
        long minutes = java.time.Duration.between(start, end).toMinutes();

        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2,
                        java.math.RoundingMode.HALF_UP);

        return resource.getPricePerHour().multiply(hours);
    }
}
