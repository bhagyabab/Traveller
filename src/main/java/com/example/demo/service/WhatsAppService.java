package com.example.demo.service;

import com.example.demo.entity.WhatsAppRequest;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/*
 * WhatsApp does NOT allow server-to-server message sending for regular numbers.
 * We build a wa.me link — frontend opens it — WhatsApp opens on the
 * traveller's phone with the message pre-filled — traveller taps Send.
 *
 * For auto-sending without user action, you would need:
 * WhatsApp Business API (Meta) or Twilio / Gupshup (paid services).
 */
@Service
public class WhatsAppService {

    // India +91 prefix
    private static final String WA_BASE = "https://wa.me/91";

    // ── 1. Pre-filled message link ────────────────────────────
    // Traveller clicks → WhatsApp opens with message to passenger
    public String buildMessageLink(WhatsAppRequest req) {
        String text = "Hi! I'm *" + req.getTravellerName() + "*, your RideShare traveller. "
                + "I am on my way to pick you up from *" + req.getStartLocation() + "* "
                + "heading to *" + req.getEndLocation() + "*. "
                + "I will be there soon! \uD83D\uDE98";   // 🚘 emoji

        return WA_BASE + req.getPassengerPhone() + "?text=" + encode(text);
    }

    // ── 2. Live location link ─────────────────────────────────
    // Traveller clicks → WhatsApp opens with Google Maps link to passenger
    public String buildLocationLink(WhatsAppRequest req) {
        String mapsUrl = "https://maps.google.com/?q="
                + req.getLatitude() + "," + req.getLongitude();

        String text = "Hi! I'm *" + req.getTravellerName() + "*, your RideShare traveller. "
                + "Here is my *live location* so you can track me: "
                + mapsUrl + " \uD83D\uDCCD\n\n"          // 📍 emoji
                + "Route: " + req.getStartLocation() + " \u2192 " + req.getEndLocation();

        return WA_BASE + req.getPassengerPhone() + "?text=" + encode(text);
    }

    // ── 3. Payment receipt link ───────────────────────────────
    // After payment confirmed → passenger gets WhatsApp receipt
    public String buildReceiptLink(String passengerPhone,
                                   String travellerName,
                                   String travellerUpi,
                                   double totalAmount,
                                   double travellerShare,
                                   String route,
                                   double distanceKm,
                                   String upiRef) {

        long driverSharePct = totalAmount > 0
                ? Math.round((travellerShare / totalAmount) * 100)
                : 0;

        String text = "\u2705 *Payment Confirmed \u2014 RideShare*\n\n"  // ✅
                + "Traveller: " + travellerName + "\n"
                + "Route: " + route + "\n"
                + "Distance: " + distanceKm + " km\n"
                + "Total paid: \u20B9" + String.format("%.0f", totalAmount) + "\n"   // ₹
                + "Driver share (" + driverSharePct + "%): \u20B9" + String.format("%.0f", travellerShare) + "\n"
                + "Driver UPI: " + travellerUpi + "\n"
                + "Transaction ref: " + upiRef + "\n\n"
                + "Thank you for riding with us! \uD83D\uDE4F";  // 🙏

        return WA_BASE + passengerPhone + "?text=" + encode(text);
    }

    // ── Helper ────────────────────────────────────────────────
    private String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}