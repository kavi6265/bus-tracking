package com.example.demo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository // ✅ Change JpaRepository to MongoRepository
public interface MapPlaceRepository extends MongoRepository<MapPlace, String> {
}
