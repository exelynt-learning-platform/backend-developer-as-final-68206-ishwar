package com.example.booking.repository;

import com.example.booking.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByActiveTrue();
    Optional<Resource> findByName(String name);
}
