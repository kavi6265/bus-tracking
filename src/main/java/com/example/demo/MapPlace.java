package com.example.demo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "map_places") // ✅ Tells Spring Boot this belongs in MongoDB
public class MapPlace {
    
    @Id // ✅ MongoDB uses String or ObjectId for IDs easily
    private String id;
    
    private String name;
    private Double latitude;
    private Double longitude;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
