package com.example.demo.entity;

// Replaces dto/PaymentRequest — lives in entity package
// Passenger sends this when clicking "Pay Now" after ride
public class PaymentRequest {

    private Long passengerId;
    private Long travellerId;
    private double distanceKm;   // actual KM driven — traveller enters this
    private int seats;           // passenger's seatsNeeded

    public PaymentRequest() {}

    public Long getPassengerId() { return passengerId; }
    public void setPassengerId(Long passengerId) { this.passengerId = passengerId; }

    public Long getTravellerId() { return travellerId; }
    public void setTravellerId(Long travellerId) { this.travellerId = travellerId; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }
}