package com.example.demo.repository;

import com.example.demo.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    // Login check
    Optional<Passenger> findByEmail(String email);

    // Check duplicate email
    boolean existsByEmail(String email);
}