package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "travellers")
public class Traveller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    @Column(name = "phone", length = 10)
    private String phno;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @Column(name = "organization")
    private String organization;

    @NotBlank(message = "Vehicle number is required")
    @Column(name = "vehicle_number", nullable = false, unique = true)
    private String vehicleNumber;

    @Min(value = 1, message = "At least 1 seat required")
    @Max(value = 4, message = "Maximum 4 seats allowed")
    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @NotBlank(message = "Start location is required")
    @Column(name = "start_location", nullable = false)
    private String startLocation;

    @NotBlank(message = "End location is required")
    @Column(name = "end_location", nullable = false)
    private String endLocation;

    @Column(name = "departure_time")
    private String departureTime;

    // Fixed ₹5 per km always
    @Column(name = "price_per_km", nullable = false)
    private Double pricePerKm = 5.0;

    @NotBlank(message = "UPI ID is required")
    @Column(name = "upi_id", nullable = false)
    private String upiId;

    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    @Column(name = "is_available")
    private boolean isAvailable = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Traveller() {}

    public Traveller(String name, String email, String phno,
                     String password, String organization,
                     String vehicleNumber, int availableSeats,
                     String startLocation, String endLocation,
                     String departureTime, String upiId,
                     String qrCodeUrl) {
        this.name = name;
        this.email = email;
        this.phno = phno;
        this.password = password;
        this.organization = organization;
        this.vehicleNumber = vehicleNumber;
        this.availableSeats = availableSeats;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.departureTime = departureTime;
        this.pricePerKm = 5.0;
        this.upiId = upiId;
        this.qrCodeUrl = qrCodeUrl;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhno() { return phno; }
    public void setPhno(String phno) { this.phno = phno; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }

    public String getEndLocation() { return endLocation; }
    public void setEndLocation(String endLocation) { this.endLocation = endLocation; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public Double getPricePerKm() { return pricePerKm; }
    public void setPricePerKm(Double pricePerKm) { this.pricePerKm = pricePerKm; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { this.isAvailable = available; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}