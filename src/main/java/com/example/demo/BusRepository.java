package com.example.demo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
public interface BusRepository extends MongoRepository<Bus, String> {
    Bus findByBusNumber(String busNumber);
    Bus findByBusNumberAndBusName(
            String busNumber,
            String busName);
    List<Bus> findBySourceContainingIgnoreCaseAndDestinationContainingIgnoreCase(
            String source,
            String destination
    );
}
