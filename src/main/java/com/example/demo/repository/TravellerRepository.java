package com.example.demo.repository;

import com.example.demo.entity.Traveller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TravellerRepository extends JpaRepository<Traveller, Long> {

    // Login check
    Optional<Traveller> findByEmail(String email);

    // Check duplicate email
    boolean existsByEmail(String email);

    // Check duplicate vehicle number
    boolean existsByVehicleNumber(String vehicleNumber);

    // Find available travellers by route
    List<Traveller> findByStartLocationAndEndLocationAndIsAvailableTrue(
        String startLocation, String endLocation
    );

    // Find all available travellers
    List<Traveller> findByIsAvailableTrue();
}