package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "passengers")
public class Passenger {

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

    @NotBlank(message = "Pickup location is required")
    @Column(name = "pickup_location", nullable = false)
    private String pickupLocation;

    @NotBlank(message = "Drop location is required")
    @Column(name = "drop_location", nullable = false)
    private String dropLocation;

    @Column(name = "preferred_time")
    private String preferredTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Min(value = 1, message = "At least 1 seat required")
    @Column(name = "seats_needed", nullable = false)
    private int seatsNeeded = 1;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Passenger() {}

    public Passenger(String name, String email, String phno,
                     String password, String organization,
                     String pickupLocation, String dropLocation,
                     String preferredTime, Gender gender,
                     int seatsNeeded) {
        this.name = name;
        this.email = email;
        this.phno = phno;
        this.password = password;
        this.organization = organization;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;
        this.preferredTime = preferredTime;
        this.gender = gender;
        this.seatsNeeded = seatsNeeded;
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

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getDropLocation() { return dropLocation; }
    public void setDropLocation(String dropLocation) { this.dropLocation = dropLocation; }

    public String getPreferredTime() { return preferredTime; }
    public void setPreferredTime(String preferredTime) { this.preferredTime = preferredTime; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public int getSeatsNeeded() { return seatsNeeded; }
    public void setSeatsNeeded(int seatsNeeded) { this.seatsNeeded = seatsNeeded; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}