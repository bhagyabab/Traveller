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
import java.time.Duration;

@Service
public class DistanceService {

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search";

    private static final String OSRM_BASE =
            "https://router.project-osrm.org/route/v1/driving/";

    // Read User-Agent from application.properties
    @Value("${app.geocoding.user-agent}")
    private String userAgent;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns driving distance in km between two place names.
     */
    public double getDistanceKm(String origin, String destination) {

        if (origin == null || origin.isBlank()
                || destination == null || destination.isBlank()) {

            throw new RuntimeException(
                    "Both pickup and drop locations are required to calculate distance"
            );
        }

        double[] originCoords = geocode(origin);
        double[] destCoords = geocode(destination);

        return route(
                originCoords,
                destCoords,
                origin,
                destination
        );
    }

    // ─────────────────────────────────────────────
    // Step 1: Place name -> Latitude / Longitude
    // ─────────────────────────────────────────────
    private double[] geocode(String place) {

        String url = NOMINATIM_URL
                + "?q=" + encode(place)
                + "&format=json"
                + "&limit=1";

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            // Check HTTP status before parsing JSON
            if (response.statusCode() != 200) {

                throw new RuntimeException(
                        "Location service returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            String responseBody = response.body();

            if (responseBody == null || responseBody.isBlank()) {

                throw new RuntimeException(
                        "Location service returned an empty response"
                );
            }

            JsonNode results;

            try {

                results = mapper.readTree(responseBody);

            } catch (Exception e) {

                String preview = responseBody.substring(
                        0,
                        Math.min(responseBody.length(), 200)
                );

                throw new RuntimeException(
                        "Location service returned an invalid response: "
                                + preview
                );
            }

            if (!results.isArray() || results.isEmpty()) {

                throw new RuntimeException(
                        "Could not find location \"" + place + "\""
                );
            }

            JsonNode first = results.get(0);

            double lat = first.path("lat").asDouble();
            double lon = first.path("lon").asDouble();

            return new double[]{lat, lon};

        } catch (RuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to look up location \""
                            + place
                            + "\": "
                            + e.getMessage()
            );
        }
    }

    // ─────────────────────────────────────────────
    // Step 2: Coordinates -> Driving Distance
    // ─────────────────────────────────────────────
    private double route(
            double[] originCoords,
            double[] destCoords,
            String origin,
            String destination) {

        // OSRM requires longitude,latitude
        String coords =
                originCoords[1] + "," + originCoords[0]
                        + ";"
                        + destCoords[1] + "," + destCoords[0];

        String url =
                OSRM_BASE
                        + coords
                        + "?overview=false";

        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("User-Agent", userAgent)
                            .header("Accept", "application/json")
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {

                throw new RuntimeException(
                        "Routing service returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            JsonNode root =
                    mapper.readTree(response.body());

            String code =
                    root.path("code").asText();

            if (!"Ok".equals(code)) {

                throw new RuntimeException(
                        "Could not find a route from \""
                                + origin
                                + "\" to \""
                                + destination
                                + "\" ("
                                + code
                                + ")"
                );
            }

            JsonNode routes =
                    root.path("routes");

            if (!routes.isArray()
                    || routes.isEmpty()) {

                throw new RuntimeException(
                        "OSRM returned no route for \""
                                + origin
                                + "\" to \""
                                + destination
                                + "\""
                );
            }

            double meters =
                    routes.get(0)
                            .path("distance")
                            .asDouble();

            // Convert meters to kilometers
            // Round to 2 decimal places
            return Math.round(
                    (meters / 1000.0) * 100.0
            ) / 100.0;

        } catch (RuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to reach routing service: "
                            + e.getMessage()
            );
        }
    }

    private String encode(String s) {

        return URLEncoder.encode(
                s,
                StandardCharsets.UTF_8
        );
    }
}