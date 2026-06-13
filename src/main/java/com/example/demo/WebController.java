package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
@Controller
public class WebController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private JavaMailSender mailSender;
    /* =========================
       AUTH PAGES
       ========================= */

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(@RequestParam String username,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String role) {

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        userRepository.save(user);

        return "redirect:/login";
    }

    /* =========================
       DASHBOARD
       ========================= */

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            Model model,
            Authentication authentication) {

        if (authentication != null) {

            userRepository.findByUsername(authentication.getName())
                    .ifPresent(user -> {

                        model.addAttribute(
                                "username",
                                user.getUsername());

                        model.addAttribute(
                                "role",
                                user.getRole());
                    });
        }

        // Dashboard Statistics
        long totalBuses = busRepository.count();

        long totalRoutes =
                busRepository.findAll()
                        .stream()
                        .map(bus ->
                                bus.getSource() + "-"
                                + bus.getDestination())
                        .distinct()
                        .count();

        long activeBuses =
                busRepository.findAll()
                        .stream()
                        .filter(bus ->
                                bus.getLatitude() != null &&
                                bus.getLongitude() != null)
                        .count();

        model.addAttribute(
                "totalBuses",
                totalBuses);

        model.addAttribute(
                "totalRoutes",
                totalRoutes);

        model.addAttribute(
                "activeBuses",
                activeBuses);

        // Show all buses on dashboard
        model.addAttribute(
                "allBuses",
                busRepository.findAll());

        // Search
        if (source != null &&
            destination != null &&
            !source.isBlank() &&
            !destination.isBlank()) {

        	List<Bus> buses =
        	        busRepository
        	                .findBySourceContainingIgnoreCaseAndDestinationContainingIgnoreCase(
        	                        source,
        	                        destination);

        	model.addAttribute("buses", buses);
        }

        return "dashboard";
    }
    /* =========================
       🚍 DRIVER – LIVE GPS UPDATE
       ========================= */
    @GetMapping("/driver")
    public String driverPage() {
        return "driver"; // This tells Spring Boot to look for "driver.html"
    }
    @PostMapping("/driver/update-location")
    @ResponseBody
    public ResponseEntity<String> updateBusLocation(
            @RequestParam String busNumber,
            @RequestParam String busName,
            @RequestParam Double sourceLatitude,
            @RequestParam Double sourceLongitude,
            @RequestParam Double destinationLatitude,
            @RequestParam Double destinationLongitude,
            @RequestParam String stops,
            Authentication authentication) {

        if (authentication == null) {

            return ResponseEntity
                    .status(401)
                    .body("UNAUTHORIZED");
        }

        Bus bus = busRepository.findByBusNumber(busNumber);

        if (bus == null) {

            return ResponseEntity
                    .badRequest()
                    .body("BUS_NOT_FOUND");
        }

        /* BUS DETAILS */

        bus.setBusName(busName);

        bus.setStops(stops);

        /* SOURCE LOCATION */

        bus.setLatitude(sourceLatitude);
        bus.setLongitude(sourceLongitude);

        /* If you have fields in Bus entity */

        bus.setSourceLatitude(sourceLatitude);
        bus.setSourceLongitude(sourceLongitude);

        bus.setDestinationLatitude(destinationLatitude);
        bus.setDestinationLongitude(destinationLongitude);

        bus.setDriverUsername(
                authentication.getName());

        bus.setLastUpdated(
                LocalDateTime.now());

        /* CURRENT STOP */

        if (stops != null && !stops.isBlank()) {

            String[] stopArray = stops.split(",");

            bus.setCurrentStop(
                    stopArray[0].trim());

            bus.setCompletedStops(0);

            bus.setDistanceRemaining(
                    (double) (stopArray.length * 5));
        }

        /* DEMO SPEED */

        bus.setSpeed(45.0);

        busRepository.save(bus);

        return ResponseEntity.ok(
                "LOCATION_UPDATED");
    }
    /* =========================
       🧍 PASSENGER – TRACK BUS
       ========================= */

    @GetMapping("/bus/location")
    @ResponseBody
    public ResponseEntity<?> getBusLocation(
            @RequestParam String busNumber) {

        Bus bus =
                busRepository.findByBusNumber(
                        busNumber);

        if (bus == null) {

            return ResponseEntity
                    .badRequest()
                    .body("BUS_NOT_FOUND");
        }

        Map<String,Object> response =
                new HashMap<>();

        String nextStop =
                "Destination Reached";

        if(bus.getStops() != null &&
           bus.getCompletedStops() != null){

            String[] stops =
                    bus.getStops().split(",");

            if(bus.getCompletedStops()
                    < stops.length - 1){

                nextStop =
                        stops[
                          bus.getCompletedStops() + 1
                        ];
            }
        }

        response.put(
                "busNumber",
                bus.getBusNumber());

        response.put(
                "busName",
                bus.getBusName());

        response.put(
                "latitude",
                bus.getLatitude());

        response.put(
                "longitude",
                bus.getLongitude());

        response.put(
                "currentStop",
                bus.getCurrentStop());

        response.put(
                "nextStop",
                nextStop);

        response.put(
                "speed",
                bus.getSpeed());

        response.put(
                "distanceRemaining",
                bus.getDistanceRemaining());

        response.put(
                "lastUpdated",
                bus.getLastUpdated());

        return ResponseEntity.ok(response);
    }
    /* =========================
       PAGE ROUTES
       ========================= */

  

    @GetMapping("/passenger")
    public String passengerPage() {
        return "passenger";
    }
    /* =========================
    🔑 FORGOT PASSWORD
    ========================= */

 @GetMapping("/forgot-password")
 public String forgotPasswordPage() {
     return "forgot-password";
 }

 @PostMapping("/forgot-password")
 public String processForgotPassword(@RequestParam String email, Model model) {

     System.out.println("Email Entered: " + email);

     Optional<User> optionalUser = userRepository.findByEmail(email);

     if (optionalUser.isEmpty()) {
         System.out.println("User not found!");
         model.addAttribute("error", "Email not found!");
         return "forgot-password";
     }

     User user = optionalUser.get();

     // Generate token
     String token = UUID.randomUUID().toString();

     System.out.println("Generated Token: " + token);

     user.setResetToken(token);
     user.setTokenExpiry(LocalDateTime.now().plusMinutes(15));

     userRepository.save(user);

     System.out.println("Token saved to database");
     System.out.println("User Email: " + user.getEmail());

     try {

         String resetLink =
                 "http://localhost:5096/reset-password?token=" + token;

         System.out.println("Reset Link: " + resetLink);

         SimpleMailMessage message = new SimpleMailMessage();
         message.setFrom("akavi6265@gmail.com");
         message.setTo(user.getEmail());
         message.setSubject("Password Reset Request");
         message.setText(
                 "Hello " + user.getUsername() + ",\n\n" +
                 "Click the link below to reset your password:\n\n" +
                 resetLink +
                 "\n\nThis link expires in 15 minutes."
         );

         mailSender.send(message);

         System.out.println("Email sent successfully!");

         model.addAttribute(
                 "message",
                 "Password reset link has been sent to your email."
         );

     } catch (Exception e) {

         System.out.println("MAIL ERROR:");
         e.printStackTrace();

         model.addAttribute(
                 "error",
                 "Failed to send email. Check Spring console."
         );
     }

     return "forgot-password";
 }
 @GetMapping("/reset-password")
 public String resetPasswordPage(@RequestParam String token, Model model) {

     System.out.println("Received Token: " + token);

     Optional<User> optionalUser = userRepository.findByResetToken(token);

     System.out.println("User Found: " + optionalUser.isPresent());

     if (optionalUser.isEmpty()) {
         model.addAttribute("error", "Invalid Token!");
         return "login";
     }

     User user = optionalUser.get();

     System.out.println("Stored Token: " + user.getResetToken());
     System.out.println("Expiry: " + user.getTokenExpiry());

     model.addAttribute("token", token);
     return "reset-password";
 }
 @PostMapping("/reset-password")
 public String processResetPassword(@RequestParam String token,
                                    @RequestParam String newPassword,
                                    Model model) {

     Optional<User> optionalUser = userRepository.findByResetToken(token);

     if (optionalUser.isEmpty()) {
         model.addAttribute("error", "Invalid password reset link!");
         return "login";
     }

     User user = optionalUser.get();

     if (user.getTokenExpiry() == null ||
         user.getTokenExpiry().isBefore(LocalDateTime.now())) {

         model.addAttribute("error", "Password reset link has expired!");
         return "login";
     }

     // Update Password
     user.setPassword(passwordEncoder.encode(newPassword));

     // Clear reset token
     user.setResetToken(null);
     user.setTokenExpiry(null);

     userRepository.save(user);

     model.addAttribute(
             "message",
             "Password updated successfully. Please login with your new password."
     );

     return "login";
 }
 @GetMapping("/driver/add-bus")
 public String addBusPage() {

     return "driver-add-bus";

 }
 @PostMapping("/driver/add-bus")
 public String saveBusDetails(@RequestParam String busNumber,
                              @RequestParam String busName,
                              @RequestParam String busType,
                              @RequestParam String source,
                              @RequestParam String destination,
                              @RequestParam String stops,
                              Authentication authentication,
                              Model model) {

     Bus existingBus =
             busRepository.findByBusNumber(
                     busNumber);

     if (existingBus != null) {

         model.addAttribute(
                 "message",
                 "Bus number already exists!");

         return "driver-add-bus";
     }

     Bus bus = new Bus();

     bus.setBusNumber(busNumber);
     bus.setBusName(busName);

     // NEW FIELD
     bus.setBusType(busType);

     bus.setSource(source);
     bus.setDestination(destination);

     bus.setStops(stops);

     // Initial Current Stop
     bus.setCurrentStop(source);

     // Tracking Data
     bus.setCompletedStops(0);

     bus.setSpeed(45.0);

     bus.setDistanceRemaining(12.5);

     // Tamil Nadu Default Location
     bus.setLatitude(11.1271);

     bus.setLongitude(78.6569);

     if (authentication != null) {

         bus.setDriverUsername(
                 authentication.getName());
     }

     bus.setLastUpdated(
             LocalDateTime.now());

     busRepository.save(bus);

     model.addAttribute(
             "message",
             "Bus details added successfully!");

     return "driver-add-bus";
 }
 @GetMapping("/track-bus")
 public String trackBus(
         @RequestParam String busNumber,
         @RequestParam String busName,
         Model model) {

     Bus bus = busRepository
             .findByBusNumberAndBusName(
                     busNumber,
                     busName);

     if (bus == null) {

         model.addAttribute(
                 "error",
                 "Bus not found");

         return "track-bus";
     }

     model.addAttribute("bus", bus);

     return "track-bus";
 }
}


