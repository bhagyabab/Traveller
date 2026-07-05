package com.example.demo.controller;

import com.example.demo.entity.Traveller;
import com.example.demo.repository.TravellerRepository;
import com.example.demo.service.TravellerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/traveller")
@CrossOrigin(origins = "*")
public class TravellerController {

    @Autowired
    private TravellerService travellerService;
    @Autowired
    private TravellerRepository travellerRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody Traveller traveller) {
        String result = travellerService.registerTraveller(traveller);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> loginData) {
        String email = loginData.get("email");
        String password = loginData.get("password");
        String result = travellerService.loginTraveller(email, password);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Long id) { // 👈 Named
        return travellerService.getTravellerById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Traveller>> getAllAvailable() {
        return ResponseEntity.ok(travellerService.getAllAvailableTravellers());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Traveller>> searchByRoute(
        @RequestParam("start") String start, // 👈 Named
        @RequestParam("end") String end) {  // 👈 Named
        return ResponseEntity.ok(travellerService.searchByRoute(start, end));
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<String> updateAvailability(
        @PathVariable("id") Long id, // 👈 Named
        @RequestParam("status") boolean status) { // 👈 Named
        return ResponseEntity.ok(travellerService.updateAvailability(id, status));
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<String> update(
        @PathVariable("id") Long id, // 👈 Named
        @Valid @RequestBody Traveller traveller) {
        return ResponseEntity.ok(travellerService.updateTraveller(id, traveller));
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) { // 👈 Named
        return ResponseEntity.ok(travellerService.deleteTraveller(id));
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<?> getByEmail(@PathVariable("email") String email) { // 👈 Named
        return travellerRepository.findByEmail(email)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<Traveller>> getAll() {
        return ResponseEntity.ok(travellerService.getAllTravellers());
    }
}
