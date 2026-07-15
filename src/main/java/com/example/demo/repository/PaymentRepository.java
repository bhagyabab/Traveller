package com.example.demo.repository;

import com.example.demo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // All payments made by a passenger (history page)
    List<Payment> findByPassengerId(Long passengerId);

    // All payments linked to a traveller
    List<Payment> findByTravellerId(Long travellerId);

    // Only PAID payments for traveller earnings screen
    List<Payment> findByTravellerIdAndStatus(Long travellerId, String status);

    // Only PAID payments for passenger receipt history
    List<Payment> findByPassengerIdAndStatus(Long passengerId, String status);

    // Find by UPI ref to avoid duplicate confirms
    Optional<Payment> findByUpiRef(String upiRef);
}