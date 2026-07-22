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

    // Bus-style fare slab — edit these in application.properties
    @Value("${app.fare.min-km:5}")
    private double minKm;

    @Value("${app.fare.min-fare:15}")
    private double minFare;

    @Value("${app.fare.rate-per-km:2.0}")
    private double ratePerKm;

    // Revenue split — must add up to 1.0
    @Value("${app.split.traveller-share:0.70}")
    private double travellerSharePct;

    @Value("${app.split.app-share:0.30}")
    private double appSharePct;

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PassengerRepository passengerRepository;
    @Autowired private TravellerRepository travellerRepository;
    @Autowired private DistanceService distanceService;

    // ─────────────────────────────────────────────────────────
    // 0. FARE CALCULATOR — bus-style slab, not flat per-km
    //    First `minKm` km cost a flat `minFare`. Every km beyond
    //    that adds `ratePerKm`. Tune these three numbers in
    //    application.properties to match your actual rates.
    // ─────────────────────────────────────────────────────────
    private double calculateFare(double distanceKm) {
        double fare;
        if (distanceKm <= minKm) {
            fare = minFare;
        } else {
            fare = minFare + (distanceKm - minKm) * ratePerKm;
        }
        // Round up to the nearest rupee, like bus fares usually are
        return Math.ceil(fare);
    }

    // Used by the /estimate endpoint so the frontend can show
    // "this distance = this much" before the passenger books,
    // without creating a Payment row.
    public double estimateFare(double distanceKm, int seats) {
        return calculateFare(distanceKm) * seats;
    }

    // Same idea, but from place names — calls Google to resolve the
    // distance first. Returns both the resolved distance and the fare
    // so the frontend can show "23 km · ₹56" before the passenger commits.
    public java.util.Map<String, Object> estimateFareByLocation(
            String origin, String destination, int seats) {
        double distanceKm = distanceService.getDistanceKm(origin, destination);
        double total = calculateFare(distanceKm) * seats;

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("distanceKm", distanceKm);
        result.put("seats", seats);
        result.put("totalAmount", total);
        return result;
    }

    // ─────────────────────────────────────────────────────────
    // 1. INITIATE — passenger clicks "Pay Now"
    //    Calculates fare, splits 50/50, builds UPI link, saves to DB
    // ─────────────────────────────────────────────────────────
    public Payment initiatePayment(PaymentRequest req) {

        Passenger passenger = passengerRepository.findById(req.getPassengerId())
                .orElseThrow(() -> new RuntimeException("Passenger not found"));

        Traveller traveller = travellerRepository.findById(req.getTravellerId())
                .orElseThrow(() -> new RuntimeException("Traveller not found"));

        // Distance = passenger's own pickup → drop, not the traveller's whole
        // route (a passenger only pays for the leg they actually ride).
        //
        // A manually supplied distanceKm (e.g. the traveller entering actual
        // km driven when marking a ride complete) is the real, ground-truth
        // figure and always wins. Only when no manual distance is supplied
        // do we fall back to the auto-geocoded estimate (e.g. when the
        // passenger initiates payment directly, before any traveller has
        // entered a real number).
        double distanceKm;
        if (req.getDistanceKm() > 0) {
            distanceKm = req.getDistanceKm();
        } else {
            try {
                distanceKm = distanceService.getDistanceKm(
                        passenger.getPickupLocation(), passenger.getDropLocation());
            } catch (RuntimeException autoCalcFailed) {
                throw new RuntimeException(
                        "Could not calculate distance automatically: " + autoCalcFailed.getMessage()
                        + ". Please enter distance manually.");
            }
        }

        // Bus-style slab fare per seat, then × seats booked (each seat = one ticket)
        double total         = calculateFare(distanceKm) * req.getSeats();
        double appShare      = Math.round(total * appSharePct * 100.0) / 100.0;
        double driverShare   = Math.round(total * travellerSharePct * 100.0) / 100.0;

        // UPI deep-link — passenger scans QR with any UPI app
        // Passenger pays YOUR app UPI (rideshare@upi) the full amount
        // You transfer driver's 70% to traveller.getUpiId() separately
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
        payment.setStartLocation(passenger.getPickupLocation());
        payment.setEndLocation(passenger.getDropLocation());
        payment.setDistanceKm(distanceKm);
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

    // ─────────────────────────────────────────────────────────
    // 7. PAYOUTS — you settle a traveller's 70% share manually
    //    (e.g. once a month) after transferring the money yourself.
    //    Requires PaymentRepository to have:
    //    List<Payment> findByTravellerIdAndStatusAndPayoutStatus(
    //        Long travellerId, String status, String payoutStatus);
    // ─────────────────────────────────────────────────────────
    public List<Payment> getPendingPayouts(Long travellerId) {
        return paymentRepository.findByTravellerIdAndStatusAndPayoutStatus(
                travellerId, "PAID", "PENDING");
    }

    public List<Payment> getSettledPayouts(Long travellerId) {
        return paymentRepository.findByTravellerIdAndStatusAndPayoutStatus(
                travellerId, "PAID", "SETTLED");
    }

    // Call this once you've actually transferred the money to the
    // traveller (e.g. at month end). Marks every unsettled PAID ride
    // for that traveller as SETTLED and returns a summary.
    // ⚠️ This should only be callable by you (an admin), not a
    // traveller or passenger — add auth/role checks before exposing
    // this in production.
    public java.util.Map<String, Object> settleTravellerPayouts(Long travellerId) {
        List<Payment> pending = getPendingPayouts(travellerId);

        double totalSettled = pending.stream()
                .mapToDouble(Payment::getTravellerShare)
                .sum();

        LocalDateTime now = LocalDateTime.now();
        for (Payment p : pending) {
            p.setPayoutStatus("SETTLED");
            p.setPayoutAt(now);
        }
        paymentRepository.saveAll(pending);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("travellerId", travellerId);
        result.put("ridesSettled", pending.size());
        result.put("totalSettled", totalSettled);
        result.put("settledAt", now);
        return result;
    }
}