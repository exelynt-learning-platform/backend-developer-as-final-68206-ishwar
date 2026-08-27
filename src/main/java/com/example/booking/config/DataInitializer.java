package com.example.booking.config;

import com.example.booking.entity.*;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               ResourceRepository resourceRepository,
                               ReservationRepository reservationRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            User admin = userRepository.findByUsername("admin").orElseGet(() ->
                    userRepository.save(new User(
                            "admin", passwordEncoder.encode("Admin@123"), Role.ADMIN)));

            User user = userRepository.findByUsername("user").orElseGet(() ->
                    userRepository.save(new User(
                            "user", passwordEncoder.encode("User@123"), Role.USER)));

            Resource room = resourceRepository.findByName("Conference Room A").orElseGet(() ->
                    resourceRepository.save(new Resource(
                            "Conference Room A", "ROOM",
                            "10-seat conference room with projector",
                            new BigDecimal("500.00"), true)));

            Resource car = resourceRepository.findByName("Company Car").orElseGet(() ->
                    resourceRepository.save(new Resource(
                            "Company Car", "VEHICLE",
                            "Sedan for official travel",
                            new BigDecimal("1200.00"), true)));

            Resource projector = resourceRepository.findByName("Projector").orElseGet(() ->
                    resourceRepository.save(new Resource(
                            "Projector", "EQUIPMENT",
                            "Full HD meeting projector",
                            new BigDecimal("300.00"), true)));

            if (reservationRepository.count() == 0) {
                reservationRepository.save(new Reservation(
                        room, user,
                        LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0),
                        LocalDateTime.now().plusDays(2).withHour(12).withMinute(0).withSecond(0),
                        new BigDecimal("1000.00"),
                        ReservationStatus.PENDING));

                reservationRepository.save(new Reservation(
                        car, user,
                        LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0),
                        LocalDateTime.now().plusDays(3).withHour(17).withMinute(0).withSecond(0),
                        new BigDecimal("9600.00"),
                        ReservationStatus.CONFIRMED));
            }
        };
    }
}
