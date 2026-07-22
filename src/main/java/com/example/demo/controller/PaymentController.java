package com.example.demo.controller;

import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentConfirmRequest;
import com.example.demo.entity.PaymentRequest;
import com.example.demo.entity.WhatsAppRequest;
import com.example.demo.service.PaymentService;
import com.example.demo.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired private PaymentService paymentService;
    @Autowired private WhatsAppService whatsAppService;

    // ── 0. Fare estimate (no DB write) ────────────────────────
    // GET /api/payment/estimate?distanceKm=8&seats=2
    // Returns: { "distanceKm":8, "seats":2, "totalAmount": 42.0 }
    // Frontend calls this to show "this distance = this much"
    // before the passenger commits to paying.
    @GetMapping("/estimate")
    public ResponseEntity<Map<String, Object>> estimate(
            @RequestParam("distanceKm") double distanceKm,
            @RequestParam(value = "seats", defaultValue = "1") int seats) {
        double total = paymentService.estimateFare(distanceKm, seats);
        return ResponseEntity.ok(Map.of(
                "distanceKm", distanceKm,
                "seats", seats,
                "totalAmount", total
        ));
    }

    // ── 0b. Fare estimate FROM PLACE NAMES (no DB write) ──────
    // GET /api/payment/estimate/by-location?origin=Ellamanchili&destination=Anakapalle&seats=2
    // Resolves distance via Google Distance Matrix, then applies the
    // same slab fare as /initiate would. Use this to show the fare
    // before the passenger books, using their saved pickup/drop.
    @GetMapping("/estimate/by-location")
    public ResponseEntity<?> estimateByLocation(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam(value = "seats", defaultValue = "1") int seats) {
        try {
            return ResponseEntity.ok(paymentService.estimateFareByLocation(origin, destination, seats));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── 1. Initiate payment ───────────────────────────────────
    // POST /api/payment/initiate
    // Body: { "passengerId":1, "travellerId":2, "distanceKm":210, "seats":1 }
    // Returns: full Payment object — use .id and .upiLink from response
    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(@RequestBody PaymentRequest req) {
        try {
            Payment payment = paymentService.initiatePayment(req);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── 2. Confirm payment ────────────────────────────────────
    // POST /api/payment/confirm
    // Body: { "paymentId":5, "upiRef":"TXN123456789" }
    // Returns: updated Payment with status=PAID
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody PaymentConfirmRequest req) {
        try {
            Payment payment = paymentService.confirmPayment(req);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── 3. Get single payment (receipt) ──────────────────────
    // GET /api/payment/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Long id) {
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── 4. Passenger payment history ─────────────────────────
    // GET /api/payment/passenger/{passengerId}
    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<Payment>> passengerHistory(
            @PathVariable("passengerId") Long passengerId) {
        return ResponseEntity.ok(paymentService.getPassengerPayments(passengerId));
    }

    // ── 5. Traveller earnings list ────────────────────────────
    // GET /api/payment/traveller/{travellerId}/earnings
    @GetMapping("/traveller/{travellerId}/earnings")
    public ResponseEntity<List<Payment>> travellerEarnings(
            @PathVariable("travellerId") Long travellerId) {
        return ResponseEntity.ok(paymentService.getTravellerEarnings(travellerId));
    }

    // ── 6. Traveller total earnings (single number) ───────────
    // GET /api/payment/traveller/{travellerId}/total
    @GetMapping("/traveller/{travellerId}/total")
    public ResponseEntity<Map<String, Double>> totalEarnings(
            @PathVariable("travellerId") Long travellerId) {
        double total = paymentService.getTotalEarnings(travellerId);
        return ResponseEntity.ok(Map.of("totalEarnings", total));
    }

    // ── 6b. Pending payout (owed, not yet paid out by you) ───
    // GET /api/payment/traveller/{travellerId}/payouts/pending
    @GetMapping("/traveller/{travellerId}/payouts/pending")
    public ResponseEntity<List<Payment>> pendingPayouts(
            @PathVariable("travellerId") Long travellerId) {
        return ResponseEntity.ok(paymentService.getPendingPayouts(travellerId));
    }

    // ── 6c. Settled payout history ────────────────────────────
    // GET /api/payment/traveller/{travellerId}/payouts/settled
    @GetMapping("/traveller/{travellerId}/payouts/settled")
    public ResponseEntity<List<Payment>> settledPayouts(
            @PathVariable("travellerId") Long travellerId) {
        return ResponseEntity.ok(paymentService.getSettledPayouts(travellerId));
    }

    // ── 6d. Settle a traveller's payout (ADMIN ACTION) ────────
    // POST /api/payment/traveller/{travellerId}/settle
    // Call this yourself once you've transferred the 70% share,
    // e.g. once a month. Marks all pending rides as SETTLED.
    // ⚠️ TODO: lock this down to admin-only before going live —
    // right now anyone who knows the URL could call it.
    @PostMapping("/traveller/{travellerId}/settle")
    public ResponseEntity<?> settlePayouts(
            @PathVariable("travellerId") Long travellerId) {
        return ResponseEntity.ok(paymentService.settleTravellerPayouts(travellerId));
    }

    // ── 7. WhatsApp message link ──────────────────────────────
    // POST /api/payment/whatsapp/message
    // Body: { "passengerPhone":"9876543210", "travellerName":"Raju",
    //         "startLocation":"Kurnool", "endLocation":"Hyderabad" }
    // Returns: { "waLink": "https://wa.me/91..." }
    // Frontend does: window.open(waLink) → WhatsApp opens on traveller phone
    @PostMapping("/whatsapp/message")
    public ResponseEntity<Map<String, String>> whatsappMessage(
            @RequestBody WhatsAppRequest req) {
        String link = whatsAppService.buildMessageLink(req);
        return ResponseEntity.ok(Map.of("waLink", link));
    }

    // ── 8. WhatsApp location link ─────────────────────────────
    // POST /api/payment/whatsapp/location
    // Body: { "passengerPhone":"9876543210", "travellerName":"Raju",
    //         "startLocation":"Kurnool", "endLocation":"Hyderabad",
    //         "latitude":15.8281, "longitude":78.0373 }
    // Returns: { "waLink": "https://wa.me/91..." }
    @PostMapping("/whatsapp/location")
    public ResponseEntity<Map<String, String>> whatsappLocation(
            @RequestBody WhatsAppRequest req) {
        String link = whatsAppService.buildLocationLink(req);
        return ResponseEntity.ok(Map.of("waLink", link));
    }

    // ── 9. WhatsApp receipt link ──────────────────────────────
    // POST /api/payment/whatsapp/receipt
    // Body: { "paymentId": 5 }
    // Returns: { "waLink": "https://wa.me/91..." }
    // Opens WhatsApp with full receipt message to passenger
    @PostMapping("/whatsapp/receipt")
    public ResponseEntity<?> whatsappReceipt(@RequestBody Map<String, Long> body) {
        Long paymentId = body.get("paymentId");
        return paymentService.getPaymentById(paymentId).map(payment -> {
            String link = whatsAppService.buildReceiptLink(
                    payment.getPassengerPhone(),
                    payment.getTravellerName(),
                    payment.getTravellerUpi(),
                    payment.getTotalAmount(),
                    payment.getTravellerShare(),
                    payment.getStartLocation() + " to " + payment.getEndLocation(),
                    payment.getDistanceKm(),
                    payment.getUpiRef()
            );
            return ResponseEntity.ok(Map.of("waLink", link));
        }).orElse(ResponseEntity.notFound().build());
    }
}