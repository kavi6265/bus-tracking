package com.example.demo;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface BusRepository extends MongoRepository<Bus, String> {
    Bus findByBusNumber(String busNumber);
    List<Bus> findBySourceIgnoreCaseAndDestinationIgnoreCase(
            String source,
            String destination);
}
