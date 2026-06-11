package com.example.demo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "buses")
public class Bus {

    @Id
    private String id;

    private String busNumber;
    private String busName;

    private String source;
    private String destination;

    // Example:
    // "Guindy,Pallavaram,Chromepet,Tambaram"
    private String stops;

    private Double latitude;
    private Double longitude;

    private String driverUsername;

    private LocalDateTime lastUpdated;
    private String currentStop;

    private Double speed;
    private Integer completedStops;
    private Double distanceRemaining;
    // =====================
    // Getters and Setters
    // =====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public String getBusName() {
        return busName;
    }

    public void setBusName(String busName) {
        this.busName = busName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getStops() {
        return stops;
    }

    public void setStops(String stops) {
        this.stops = stops;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDriverUsername() {
        return driverUsername;
    }

    public void setDriverUsername(String driverUsername) {
        this.driverUsername = driverUsername;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    public String getCurrentStop() {
        return currentStop;
    }

    public void setCurrentStop(String currentStop) {
        this.currentStop = currentStop;
    }
    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Integer getCompletedStops() {
        return completedStops;
    }

    public void setCompletedStops(Integer completedStops) {
        this.completedStops = completedStops;
    }

    public Double getDistanceRemaining() {
        return distanceRemaining;
    }

    public void setDistanceRemaining(Double distanceRemaining) {
        this.distanceRemaining = distanceRemaining;
    }
}