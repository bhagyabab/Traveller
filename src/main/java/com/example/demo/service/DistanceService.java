package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/*
 * FREE distance calculation — no API key, no billing.
 *
 * Step 1: Nominatim (OpenStreetMap) geocodes place names -> lat/lng.
 *         https://nominatim.org/release-docs/latest/api/Search/
 *         Usage policy: max ~1 request/second, must send a real
 *         User-Agent identifying your app (they will block generic
 *         ones). Not for very high traffic — see note below.
 *
 * Step 2: OSRM public demo server computes driving distance between
 *         the two coordinates.
 *         http://project-osrm.org/docs/v5.24.0/api/#route-service
 *         This is a shared demo server meant for light/testing use,
 *         not guaranteed uptime or rate limits for production.
 *
 * If you outgrow the free public servers, you can self-host OSRM
 * (still free, open source, Docker image available) and just change
 * OSRM_BASE below to your own server.
 */
@Service
public class DistanceService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String OSRM_BASE = "https://router.project-osrm.org/route/v1/driving/";

    // Nominatim requires a descriptive User-Agent (not the default Java one)
    private static final String USER_AGENT = "RideShareApp/1.0 (contact: youremail@example.com)";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns driving distance in km between two place names.
     * Throws RuntimeException with a clear, user-facing message if either
     * place can't be geocoded or the route can't be resolved — callers
     * should catch this and decide whether to fall back to a manually
     * entered distance.
     */
    public double getDistanceKm(String origin, String destination) {
        if (origin == null || origin.isBlank() || destination == null || destination.isBlank()) {
            throw new RuntimeException("Both pickup and drop locations are required to calculate distance");
        }

        double[] originCoords = geocode(origin);
        double[] destCoords = geocode(destination);

        return route(originCoords, destCoords, origin, destination);
    }

    // ── Step 1: place name -> [lat, lon] via Nominatim ──────────
    private double[] geocode(String place) {
        String url = NOMINATIM_URL
                + "?q=" + encode(place)
                + "&format=json"
                + "&limit=1";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode results = mapper.readTree(response.body());

            if (!results.isArray() || results.isEmpty()) {
                throw new RuntimeException("Could not find location \"" + place + "\"");
            }

            JsonNode first = results.get(0);
            double lat = first.path("lat").asDouble();
            double lon = first.path("lon").asDouble();
            return new double[]{lat, lon};

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to look up location \"" + place + "\": " + e.getMessage());
        }
    }

    // ── Step 2: two coordinate pairs -> driving distance via OSRM ─
    private double route(double[] originCoords, double[] destCoords, String origin, String destination) {
        // OSRM wants lon,lat order (not lat,lon)
        String coords = originCoords[1] + "," + originCoords[0]
                + ";" + destCoords[1] + "," + destCoords[0];

        String url = OSRM_BASE + coords + "?overview=false";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());

            String code = root.path("code").asText();
            if (!"Ok".equals(code)) {
                throw new RuntimeException("Could not find a route from \"" + origin
                        + "\" to \"" + destination + "\" (" + code + ")");
            }

            JsonNode routes = root.path("routes");
            if (!routes.isArray() || routes.isEmpty()) {
                throw new RuntimeException("OSRM returned no route for \""
                        + origin + "\" to \"" + destination + "\"");
            }

            double meters = routes.get(0).path("distance").asDouble();
            return Math.round((meters / 1000.0) * 100.0) / 100.0; // km, 2 decimal places

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to reach routing service: " + e.getMessage());
        }
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}