package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;

    @Column(name = "passenger_name", nullable = false)
    private String passengerName;

    @Column(name = "passenger_phone", nullable = false)
    private String passengerPhone;

    @Column(name = "traveller_id", nullable = false)
    private Long travellerId;

    @Column(name = "traveller_name", nullable = false)
    private String travellerName;

    @Column(name = "traveller_upi", nullable = false)
    private String travellerUpi;

    @Column(name = "start_location", nullable = false)
    private String startLocation;

    @Column(name = "end_location", nullable = false)
    private String endLocation;

    @Column(name = "distance_km", nullable = false)
    private double distanceKm;

    @Column(name = "seats", nullable = false)
    private int seats;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "app_share", nullable = false)
    private double appShare;          // your cut — see app.split.app-share in application.properties

    @Column(name = "traveller_share", nullable = false)
    private double travellerShare;    // traveller's cut — see app.split.traveller-share in application.properties

    @Column(name = "upi_ref")
    private String upiRef;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, PAID, FAILED

    // Tracks whether YOU have paid the traveller their 70% share yet.
    // Only meaningful once status = PAID. PENDING = owed but not yet paid out,
    // SETTLED = you've transferred the traveller's share (e.g. monthly).
    @Column(name = "payout_status", nullable = false)
    private String payoutStatus = "PENDING";

    @Column(name = "payout_at")
    private LocalDateTime payoutAt;

    @Column(name = "upi_link", length = 600)
    private String upiLink;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public Payment() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPassengerId() { return passengerId; }
    public void setPassengerId(Long passengerId) { this.passengerId = passengerId; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getPassengerPhone() { return passengerPhone; }
    public void setPassengerPhone(String passengerPhone) { this.passengerPhone = passengerPhone; }

    public Long getTravellerId() { return travellerId; }
    public void setTravellerId(Long travellerId) { this.travellerId = travellerId; }

    public String getTravellerName() { return travellerName; }
    public void setTravellerName(String travellerName) { this.travellerName = travellerName; }

    public String getTravellerUpi() { return travellerUpi; }
    public void setTravellerUpi(String travellerUpi) { this.travellerUpi = travellerUpi; }

    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }

    public String getEndLocation() { return endLocation; }
    public void setEndLocation(String endLocation) { this.endLocation = endLocation; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getAppShare() { return appShare; }
    public void setAppShare(double appShare) { this.appShare = appShare; }

    public double getTravellerShare() { return travellerShare; }
    public void setTravellerShare(double travellerShare) { this.travellerShare = travellerShare; }

    public String getUpiRef() { return upiRef; }
    public void setUpiRef(String upiRef) { this.upiRef = upiRef; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUpiLink() { return upiLink; }
    public void setUpiLink(String upiLink) { this.upiLink = upiLink; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(String payoutStatus) { this.payoutStatus = payoutStatus; }

    public LocalDateTime getPayoutAt() { return payoutAt; }
    public void setPayoutAt(LocalDateTime payoutAt) { this.payoutAt = payoutAt; }
}