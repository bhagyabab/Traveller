package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/*
 * Turns two place names (e.g. "Ellamanchili", "Anakapalle") into a driving
 * distance in km, using Google's Distance Matrix API.
 *
 * Requires GOOGLE_MAPS_API_KEY to be set as an environment variable —
 * never hardcode the key in application.properties or commit it to git.
 * Enable "Distance Matrix API" for that key in Google Cloud Console,
 * and set up billing (Google requires it even for free-tier usage).
 */
@Service
public class DistanceService {

    @Value("${google.maps.api-key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns driving distance in km between two place names.
     * Throws RuntimeException with a clear, user-facing message if the
     * key is missing, the route can't be resolved, or the call fails —
     * callers should catch this and decide whether to fall back to a
     * manually entered distance.
     */
    public double getDistanceKm(String origin, String destination) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Google Maps API key is not configured (google.maps.api-key)");
        }
        if (origin == null || origin.isBlank() || destination == null || destination.isBlank()) {
            throw new RuntimeException("Both pickup and drop locations are required to calculate distance");
        }

        String url = "https://maps.googleapis.com/maps/api/distancematrix/json"
                + "?origins=" + encode(origin)
                + "&destinations=" + encode(destination)
                + "&units=metric"
                + "&key=" + apiKey;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                String errorMsg = root.path("error_message").asText("");
                throw new RuntimeException("Google Maps API error: " + status
                        + (errorMsg.isBlank() ? "" : " — " + errorMsg));
            }

            JsonNode rows = root.path("rows");
            if (!rows.isArray() || rows.isEmpty()) {
                throw new RuntimeException("Google Maps returned no route data for \""
                        + origin + "\" to \"" + destination + "\"");
            }

            JsonNode element = rows.get(0).path("elements").get(0);
            String elementStatus = element.path("status").asText();

            if (!"OK".equals(elementStatus)) {
                throw new RuntimeException("Could not find a route from \"" + origin
                        + "\" to \"" + destination + "\" (" + elementStatus + ")");
            }

            double meters = element.path("distance").path("value").asDouble();
            return Math.round((meters / 1000.0) * 100.0) / 100.0; // km, 2 decimal places

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to reach Google Maps: " + e.getMessage());
        }
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}