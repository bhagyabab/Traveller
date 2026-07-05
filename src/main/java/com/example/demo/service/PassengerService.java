package com.example.demo.service;

import com.example.demo.entity.Passenger;
import com.example.demo.repository.PassengerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PassengerService {

    @Autowired
    private PassengerRepository passengerRepository;

    public String registerPassenger(Passenger passenger) {
        if (passengerRepository.existsByEmail(passenger.getEmail())) {
            return "Email already registered!";
        }
        passengerRepository.save(passenger);
        return "Passenger registered successfully!";
    }

    public String loginPassenger(String email, String password) {
        Optional<Passenger> passenger = passengerRepository.findByEmail(email);
        if (passenger.isEmpty()) return "Email not found!";
        if (!password.equals(passenger.get().getPassword())) {
            return "Incorrect password!";
        }
        return "Login successful! ID:" + passenger.get().getId();
    }

    public Optional<Passenger> getPassengerById(Long id) {
        return passengerRepository.findById(id);
    }

    public List<Passenger> getAllPassengers() {
        return passengerRepository.findAll();
    }

    public String updatePassenger(Long id, Passenger updatedData) {
        Optional<Passenger> existing = passengerRepository.findById(id);
        if (existing.isEmpty()) return "Passenger not found!";
        Passenger passenger = existing.get();
        passenger.setName(updatedData.getName());
        passenger.setPhno(updatedData.getPhno());
        passenger.setOrganization(updatedData.getOrganization());
        passenger.setPickupLocation(updatedData.getPickupLocation());
        passenger.setDropLocation(updatedData.getDropLocation());
        passenger.setPreferredTime(updatedData.getPreferredTime());
        passenger.setGender(updatedData.getGender());
        passenger.setSeatsNeeded(updatedData.getSeatsNeeded());
        passengerRepository.save(passenger);
        return "Profile updated successfully!";
    }

    public String deletePassenger(Long id) {
        if (!passengerRepository.existsById(id)) return "Passenger not found!";
        passengerRepository.deleteById(id);
        return "Passenger deleted successfully!";
    }
}