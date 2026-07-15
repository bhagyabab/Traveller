package com.example.demo.service;

import com.example.demo.entity.Passenger;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentConfirmRequest;
import com.example.demo.entity.PaymentRequest;
import com.example.demo.entity.Traveller;
import com.example.demo.repository.PassengerRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.TravellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    // Set in application.properties: app.upi.id=rideshare@upi
    @Value("${app.upi.id:rideshare@upi}")
    private String appUpiId;

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PassengerRepository passengerRepository;
    @Autowired private TravellerRepository travellerRepository;

    // ─────────────────────────────────────────────────────────
    // 1. INITIATE — passenger clicks "Pay Now"
    //    Calculates fare, splits 50/50, builds UPI link, saves to DB
    // ─────────────────────────────────────────────────────────
    public Payment initiatePayment(PaymentRequest req) {

        Passenger passenger = passengerRepository.findById(req.getPassengerId())
                .orElseThrow(() -> new RuntimeException("Passenger not found"));

        Traveller traveller = travellerRepository.findById(req.getTravellerId())
                .orElseThrow(() -> new RuntimeException("Traveller not found"));

        // Fare: ₹5 per km × seats
        double total         = req.getDistanceKm() * 5.0 * req.getSeats();
        double appShare      = Math.round(total * 0.50 * 100.0) / 100.0;
        double driverShare   = Math.round(total * 0.50 * 100.0) / 100.0;

        // UPI deep-link — passenger scans QR with any UPI app
        // Passenger pays YOUR app UPI (rideshare@upi) the full amount
        // You transfer driver's 50% to traveller.getUpiId() separately
        String upiLink = "upi://pay"
                + "?pa="  + appUpiId
                + "&pn=RideShare"
                + "&am="  + String.format("%.2f", total)
                + "&tn=RideShare+Fare"
                + "&cu=INR";

        Payment payment = new Payment();
        payment.setPassengerId(passenger.getId());
        payment.setPassengerName(passenger.getName());
        payment.setPassengerPhone(passenger.getPhno());
        payment.setTravellerId(traveller.getId());
        payment.setTravellerName(traveller.getName());
        payment.setTravellerUpi(traveller.getUpiId());
        payment.setStartLocation(traveller.getStartLocation());
        payment.setEndLocation(traveller.getEndLocation());
        payment.setDistanceKm(req.getDistanceKm());
        payment.setSeats(req.getSeats());
        payment.setTotalAmount(total);
        payment.setAppShare(appShare);
        payment.setTravellerShare(driverShare);
        payment.setUpiLink(upiLink);
        payment.setStatus("PENDING");

        return paymentRepository.save(payment);
    }

    // ─────────────────────────────────────────────────────────
    // 2. CONFIRM — passenger confirms after paying in UPI app
    //    Marks PAID, records UTR ref and timestamp
    // ─────────────────────────────────────────────────────────
    public Payment confirmPayment(PaymentConfirmRequest req) {

        Payment payment = paymentRepository.findById(req.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        if ("PAID".equals(payment.getStatus())) {
            throw new RuntimeException("Payment already confirmed");
        }

        // Use UTR ref from passenger or auto-generate one
        String ref = (req.getUpiRef() != null && !req.getUpiRef().isBlank())
                ? req.getUpiRef()
                : "TXN" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 12).toUpperCase();

        payment.setUpiRef(ref);
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    // ─────────────────────────────────────────────────────────
    // 3. GET single payment by ID (for receipt screen)
    // ─────────────────────────────────────────────────────────
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    // ─────────────────────────────────────────────────────────
    // 4. GET all payments for a passenger (history)
    // ─────────────────────────────────────────────────────────
    public List<Payment> getPassengerPayments(Long passengerId) {
        return paymentRepository.findByPassengerId(passengerId);
    }

    // ─────────────────────────────────────────────────────────
    // 5. GET confirmed earnings for a traveller
    // ─────────────────────────────────────────────────────────
    public List<Payment> getTravellerEarnings(Long travellerId) {
        return paymentRepository.findByTravellerIdAndStatus(travellerId, "PAID");
    }

    // ─────────────────────────────────────────────────────────
    // 6. GET total money earned by traveller (single number)
    // ─────────────────────────────────────────────────────────
    public double getTotalEarnings(Long travellerId) {
        return paymentRepository
                .findByTravellerIdAndStatus(travellerId, "PAID")
                .stream()
                .mapToDouble(Payment::getTravellerShare)
                .sum();
    }
}