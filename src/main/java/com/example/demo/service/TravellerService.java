package com.example.demo.service;

import com.example.demo.entity.Traveller;
import com.example.demo.repository.TravellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TravellerService {

    @Autowired
    private TravellerRepository travellerRepository;

    public String registerTraveller(Traveller traveller) {
        if (travellerRepository.existsByEmail(traveller.getEmail())) {
            return "Email already registered!";
        }
        if (travellerRepository.existsByVehicleNumber(traveller.getVehicleNumber())) {
            return "Vehicle number already registered!";
        }
        traveller.setPricePerKm(5.0);
        travellerRepository.save(traveller);
        return "Traveller registered successfully!";
    }

    public String loginTraveller(String email, String password) {
        Optional<Traveller> traveller = travellerRepository.findByEmail(email);
        if (traveller.isEmpty()) return "Email not found!";
        if (!password.equals(traveller.get().getPassword())) {
            return "Incorrect password!";
        }
        return "Login successful! ID:" + traveller.get().getId();
    }

    public Optional<Traveller> getTravellerById(Long id) {
        return travellerRepository.findById(id);
    }

    public List<Traveller> getAllAvailableTravellers() {
        return travellerRepository.findByIsAvailableTrue();
    }

    public List<Traveller> getAllTravellers() {
        return travellerRepository.findAll();
    }

    public List<Traveller> searchByRoute(String startLocation, String endLocation) {
        return travellerRepository
            .findByStartLocationAndEndLocationAndIsAvailableTrue(startLocation, endLocation);
    }

    public String updateAvailability(Long id, boolean status) {
        Optional<Traveller> traveller = travellerRepository.findById(id);
        if (traveller.isEmpty()) return "Traveller not found!";
        traveller.get().setAvailable(status);
        travellerRepository.save(traveller.get());
        return "Availability updated!";
    }

    public String updateTraveller(Long id, Traveller updatedData) {
        Optional<Traveller> existing = travellerRepository.findById(id);
        if (existing.isEmpty()) return "Traveller not found!";
        Traveller traveller = existing.get();
        traveller.setName(updatedData.getName());
        traveller.setPhno(updatedData.getPhno());
        traveller.setOrganization(updatedData.getOrganization());
        traveller.setVehicleNumber(updatedData.getVehicleNumber());
        traveller.setAvailableSeats(updatedData.getAvailableSeats());
        traveller.setStartLocation(updatedData.getStartLocation());
        traveller.setEndLocation(updatedData.getEndLocation());
        traveller.setDepartureTime(updatedData.getDepartureTime());
        traveller.setUpiId(updatedData.getUpiId());
        traveller.setQrCodeUrl(updatedData.getQrCodeUrl());
        travellerRepository.save(traveller);
        return "Profile updated successfully!";
    }

    public String deleteTraveller(Long id) {
        if (!travellerRepository.existsById(id)) return "Traveller not found!";
        travellerRepository.deleteById(id);
        return "Traveller deleted successfully!";
    }
}