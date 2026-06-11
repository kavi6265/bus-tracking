package com.example.demo;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    User findByPhoneNumber(String phoneNumber);

    Optional<User> findByResetToken(String resetToken);
}