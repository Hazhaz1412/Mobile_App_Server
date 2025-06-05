package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeocodingService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public String getAddressFromCoordinates(double latitude, double longitude) {
        try {
            // Sử dụng Nominatim (OpenStreetMap) API - miễn phí
            String url = String.format(
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&accept-language=vi",
                latitude, longitude
            );
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);
            
            return node.path("display_name").asText();
            
        } catch (Exception e) {
            return String.format("Lat: %.6f, Lng: %.6f", latitude, longitude);
        }
    }
}