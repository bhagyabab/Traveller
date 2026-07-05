package com.example.demo.controller;

import com.example.demo.entity.Passenger;
import com.example.demo.repository.PassengerRepository;
import com.example.demo.service.PassengerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passenger")
@CrossOrigin(origins = "*")
public class PassengerController {

    @Autowired
    private PassengerService passengerService;
    @Autowired
    private PassengerRepository passengerRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody Passenger passenger) {
        String result = passengerService.registerPassenger(passenger);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> loginData) {
        String email = loginData.get("email");
        String password = loginData.get("password");
        String result = passengerService.loginPassenger(email, password);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Long id) { // 👈 Named
        return passengerService.getPassengerById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<String> update(
        @PathVariable("id") Long id, // 👈 Named
        @Valid @RequestBody Passenger passenger) {
        return ResponseEntity.ok(passengerService.updatePassenger(id, passenger));
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<?> getByEmail(@PathVariable("email") String email) { // 👈 Named
        return passengerRepository.findByEmail(email)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) { // 👈 Named
        return ResponseEntity.ok(passengerService.deletePassenger(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Passenger>> getAll() {
        return ResponseEntity.ok(passengerService.getAllPassengers());
    }
}
