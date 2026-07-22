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

@Service
public class DistanceService {

    // ============================================================
    // PHOTON GEOCODING API
    // Converts place names into Latitude / Longitude
    // Uses OpenStreetMap data
    // No API key required
    // ============================================================
    private static final String PHOTON_URL =
            "https://photon.komoot.io/api/";

    // ============================================================
    // OSRM ROUTING API
    // Calculates driving distance between coordinates
    // ============================================================
    private static final String OSRM_BASE =
            "https://router.project-osrm.org/route/v1/driving/";

    // HTTP Client
    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

    // JSON Parser
    private final ObjectMapper mapper =
            new ObjectMapper();


    // ============================================================
    // MAIN METHOD
    // ============================================================

    /**
     * Returns driving distance in kilometers between
     * two place names.
     *
     * Example:
     *
     * origin      = Yelamanchili, Andhra Pradesh, India
     * destination = Anakapalle, Andhra Pradesh, India
     *
     * Flow:
     *
     * Place Name
     *      ↓
     * Photon Geocoding
     *      ↓
     * Latitude + Longitude
     *      ↓
     * OSRM Routing
     *      ↓
     * Driving Distance in KM
     */
    public double getDistanceKm(
            String origin,
            String destination) {

        // Validate input
        if (origin == null
                || origin.isBlank()
                || destination == null
                || destination.isBlank()) {

            throw new RuntimeException(
                    "Both pickup and drop locations are required to calculate distance"
            );
        }

        // Get pickup coordinates
        double[] originCoords =
                geocode(origin);

        // Get destination coordinates
        double[] destinationCoords =
                geocode(destination);

        // Calculate driving distance
        return route(
                originCoords,
                destinationCoords,
                origin,
                destination
        );
    }


    // ============================================================
    // STEP 1: GEOCODING USING PHOTON
    // ============================================================

    /**
     * Converts place name into:
     *
     * [latitude, longitude]
     *
     * Photon GeoJSON coordinates are:
     *
     * [longitude, latitude]
     */
    private double[] geocode(String place) {

        // Build Photon URL
        String url =
                PHOTON_URL
                        + "?q="
                        + encode(place)
                        + "&limit=1"
                        + "&lang=en";

        try {

            System.out.println(
                    "Photon request: " + url
            );

            // Create HTTP request
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .GET()
                            .build();

            // Send HTTP request
            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Photon HTTP Status: "
                            + response.statusCode()
            );

            // Check HTTP status
            if (response.statusCode() != 200) {

                throw new RuntimeException(
                        "Photon location service returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            // Get response body
            String responseBody =
                    response.body();

            // Check empty response
            if (responseBody == null
                    || responseBody.isBlank()) {

                throw new RuntimeException(
                        "Photon returned an empty response for location: "
                                + place
                );
            }

            // Parse JSON
            JsonNode root;

            try {

                root =
                        mapper.readTree(responseBody);

            } catch (Exception e) {

                String preview =
                        responseBody.substring(
                                0,
                                Math.min(
                                        responseBody.length(),
                                        500
                                )
                        );

                throw new RuntimeException(
                        "Photon returned an invalid response for location '"
                                + place
                                + "'. Response: "
                                + preview
                );
            }

            // Get GeoJSON features
            JsonNode features =
                    root.path("features");

            // Check results
            if (!features.isArray()
                    || features.isEmpty()) {

                throw new RuntimeException(
                        "Could not find location: "
                                + place
                );
            }

            // Get first result
            JsonNode firstFeature =
                    features.get(0);

            // Get geometry
            JsonNode geometry =
                    firstFeature.path(
                            "geometry"
                    );

            // Get coordinates
            JsonNode coordinates =
                    geometry.path(
                            "coordinates"
                    );

            // Validate coordinates
            if (!coordinates.isArray()
                    || coordinates.size() < 2) {

                throw new RuntimeException(
                        "Photon did not return valid coordinates for: "
                                + place
                );
            }

            /*
             * Photon / GeoJSON format:
             *
             * coordinates[0] = longitude
             * coordinates[1] = latitude
             */

            double longitude =
                    coordinates
                            .get(0)
                            .asDouble();

            double latitude =
                    coordinates
                            .get(1)
                            .asDouble();

            System.out.println(
                    "Location found: "
                            + place
                            + " -> Latitude: "
                            + latitude
                            + ", Longitude: "
                            + longitude
            );

            /*
             * Return:
             *
             * [latitude, longitude]
             */
            return new double[]{
                    latitude,
                    longitude
            };

        } catch (InterruptedException e) {

            // Restore interrupted state
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Photon location lookup was interrupted for: "
                            + place
            );

        } catch (RuntimeException e) {

            // Keep original error
            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to look up location '"
                            + place
                            + "' using Photon: "
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // STEP 2: ROUTING USING OSRM
    // ============================================================

    /**
     * Calculates driving distance between
     * two coordinate pairs.
     *
     * OSRM requires:
     *
     * longitude,latitude
     */
    private double route(
            double[] originCoords,
            double[] destinationCoords,
            String origin,
            String destination) {

        /*
         * originCoords:
         *
         * [latitude, longitude]
         *
         * destinationCoords:
         *
         * [latitude, longitude]
         *
         * OSRM requires:
         *
         * longitude,latitude
         */

        String coordinates =
                originCoords[1]
                        + ","
                        + originCoords[0]
                        + ";"
                        + destinationCoords[1]
                        + ","
                        + destinationCoords[0];

        // Build OSRM URL
        String url =
                OSRM_BASE
                        + coordinates
                        + "?overview=false";

        try {

            System.out.println(
                    "OSRM request: " + url
            );

            // Create HTTP request
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .GET()
                            .build();

            // Send HTTP request
            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "OSRM HTTP Status: "
                            + response.statusCode()
            );

            // Check HTTP status
            if (response.statusCode() != 200) {

                throw new RuntimeException(
                        "OSRM routing service returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body()
                );
            }

            // Parse JSON response
            JsonNode root =
                    mapper.readTree(
                            response.body()
                    );

            // Get OSRM status
            String code =
                    root.path("code")
                            .asText();

            // Check route status
            if (!"Ok".equals(code)) {

                throw new RuntimeException(
                        "Could not find a driving route from '"
                                + origin
                                + "' to '"
                                + destination
                                + "'. OSRM status: "
                                + code
                );
            }

            // Get routes
            JsonNode routes =
                    root.path("routes");

            // Check routes
            if (!routes.isArray()
                    || routes.isEmpty()) {

                throw new RuntimeException(
                        "OSRM returned no route from '"
                                + origin
                                + "' to '"
                                + destination
                                + "'"
                );
            }

            // Get first/best route
            JsonNode firstRoute =
                    routes.get(0);

            // Distance returned by OSRM is in meters
            double meters =
                    firstRoute
                            .path("distance")
                            .asDouble();

            // Convert meters to kilometers
            double kilometers =
                    meters / 1000.0;

            // Round to 2 decimal places
            double roundedKm =
                    Math.round(
                            kilometers * 100.0
                    ) / 100.0;

            System.out.println(
                    "Driving distance: "
                            + roundedKm
                            + " km"
            );

            return roundedKm;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "OSRM routing request was interrupted"
            );

        } catch (RuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to calculate driving distance from '"
                            + origin
                            + "' to '"
                            + destination
                            + "': "
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // URL ENCODING
    // ============================================================

    private String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}