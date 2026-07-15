package com.example.demo.entity;

// Replaces dto/WhatsAppRequest — lives in entity package
// Traveller sends this to generate a wa.me link for the passenger
public class WhatsAppRequest {

    private String passengerPhone;   // 10-digit number
    private String travellerName;
    private String startLocation;
    private String endLocation;
    private String type;             // "MESSAGE" or "LOCATION"
    private double latitude;         // only needed when type = LOCATION
    private double longitude;        // only needed when type = LOCATION

    public WhatsAppRequest() {}

    public String getPassengerPhone() { return passengerPhone; }
    public void setPassengerPhone(String passengerPhone) { this.passengerPhone = passengerPhone; }

    public String getTravellerName() { return travellerName; }
    public void setTravellerName(String travellerName) { this.travellerName = travellerName; }

    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }

    public String getEndLocation() { return endLocation; }
    public void setEndLocation(String endLocation) { this.endLocation = endLocation; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}