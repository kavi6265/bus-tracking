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

    @Autowired
    private MapPlaceRepository mapPlaceRepository;

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
                        model.addAttribute("username", user.getUsername());
                        model.addAttribute("role", user.getRole());
                    });
        }

        // Dashboard Statistics
        long totalBuses = busRepository.count();

        long totalRoutes = busRepository.findAll().stream()
                        .map(bus -> bus.getSource() + "-" + bus.getDestination())
                        .distinct()
                        .count();

        long activeBuses = busRepository.findAll().stream()
                        .filter(bus -> bus.getLatitude() != null && bus.getLongitude() != null)
                        .count();

        model.addAttribute("totalBuses", totalBuses);
        model.addAttribute("totalRoutes", totalRoutes);
        model.addAttribute("activeBuses", activeBuses);

        // Show all buses on dashboard
        model.addAttribute("allBuses", busRepository.findAll());

        // Search
        if (source != null && destination != null && !source.isBlank() && !destination.isBlank()) {
            List<Bus> buses = busRepository.findBySourceContainingIgnoreCaseAndDestinationContainingIgnoreCase(source, destination);
            model.addAttribute("buses", buses);
        }

        return "dashboard";
    }

    /* =========================
       🚍 DRIVER – LIVE GPS UPDATE
       ========================= */
    @GetMapping("/driver")
    public String driverPage() {
        return "driver"; 
    }

    /* =========================
    🚍 DRIVER – LIVE GPS UPDATE
    ========================= */
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
         @RequestParam(required = false) String currentStop, // ✅ Added parameter to accept driver radar stop
         @RequestParam(required = false) Double speed, 
         Authentication authentication) {

     if (authentication == null) {
         return ResponseEntity.status(401).body("UNAUTHORIZED");
     }

     Bus bus = busRepository.findByBusNumber(busNumber);

     if (bus == null) {
         return ResponseEntity.badRequest().body("BUS_NOT_FOUND");
     }

     /* BUS DETAILS */
     bus.setBusName(busName);
     bus.setStops(stops);

     /* LIVE TELEMETRY COORDINATES */
     bus.setLatitude(sourceLatitude);
     bus.setLongitude(sourceLongitude);

     bus.setSourceLatitude(sourceLatitude);
     bus.setSourceLongitude(sourceLongitude);
     bus.setDestinationLatitude(destinationLatitude);
     bus.setDestinationLongitude(destinationLongitude);

     bus.setDriverUsername(authentication.getName());
     bus.setLastUpdated(LocalDateTime.now());

     /* 📍 INTELLIGENT RADAR & TIMELINE STATE SYNC */
     if (stops != null && !stops.isBlank()) {
         String[] stopArray = stops.split(",");
         
         // If the driver's frontend sent a computed stop name, use it. Otherwise, fall back to first stop.
         if (currentStop != null && !currentStop.isBlank()) {
             bus.setCurrentStop(currentStop.trim());
             
             // Automatically find how many stops are behind the current one
             int computedIndex = 0;
             for (int i = 0; i < stopArray.length; i++) {
                 if (stopArray[i].trim().equalsIgnoreCase(currentStop.trim())) {
                     computedIndex = i;
                     break;
                 }
             }
             bus.setCompletedStops(computedIndex);
             
             // Dynamically reduce remaining distance based on progress (Assuming ~5km chunks between segments)
             double totalStopsLeft = (stopArray.length - 1) - computedIndex;
             bus.setDistanceRemaining(Math.max(0.0, totalStopsLeft * 5.0));
         } else {
             // Fallback implementation
             bus.setCurrentStop(stopArray[0].trim());
             bus.setCompletedStops(0);
             bus.setDistanceRemaining((double) (stopArray.length * 5.0));
         }
     }

     /* LIVE SPEED TRANSITION */
     if (speed != null) {
         bus.setSpeed(speed);
     } else {
         bus.setSpeed(45.0); 
     }

     busRepository.save(bus);

     return ResponseEntity.ok("LOCATION_UPDATED");
 }

    /* =========================
       🧍 PASSENGER – TRACK BUS
       ========================= */
    @GetMapping("/bus/location")
    @ResponseBody
    public ResponseEntity<?> getBusLocation(@RequestParam String busNumber) {

        Bus bus = busRepository.findByBusNumber(busNumber);

        if (bus == null) {
            return ResponseEntity.badRequest().body("BUS_NOT_FOUND");
        }

        Map<String, Object> response = new HashMap<>();

        String nextStop = "Destination Reached";

        if (bus.getStops() != null && bus.getCompletedStops() != null) {
            String[] stops = bus.getStops().split(",");
            if (bus.getCompletedStops() < stops.length - 1) {
                nextStop = stops[bus.getCompletedStops() + 1];
            }
        }

        response.put("busNumber", bus.getBusNumber());
        response.put("busName", bus.getBusName());
        
        // CRITICAL MAP SYNC NODES: Send both fields so passenger Leaflet script captures coords smoothly
        response.put("latitude", bus.getLatitude());
        response.put("longitude", bus.getLongitude());
        response.put("sourceLatitude", bus.getSourceLatitude());
        response.put("sourceLongitude", bus.getSourceLongitude());
        
        response.put("currentStop", bus.getCurrentStop());
        response.put("nextStop", nextStop);
        response.put("speed", bus.getSpeed());
        response.put("distanceRemaining", bus.getDistanceRemaining());
        response.put("lastUpdated", bus.getLastUpdated());

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
        String token = UUID.randomUUID().toString();
        System.out.println("Generated Token: " + token);

        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        try {
            String resetLink = "https://bus-tracking-3-0idg.onrender.com/reset-password?token=" + token;
            System.out.println("Reset Link: " + resetLink);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("akavi6265@gmail.com");
            message.setTo(user.getEmail());
            message.setSubject("Password Reset Request");
            message.setText("Hello " + user.getUsername() + ",\n\n" +
                            "Click the link below to reset your password:\n\n" +
                            resetLink + "\n\nThis link expires in 15 minutes.");

            mailSender.send(message);
            model.addAttribute("message", "Password reset link has been sent to your email.");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to send email. Check Spring console.");
        }

        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        Optional<User> optionalUser = userRepository.findByResetToken(token);
        if (optionalUser.isEmpty()) {
            model.addAttribute("error", "Invalid Token!");
            return "login";
        }
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

        if (user.getTokenExpiry() == null || user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Password reset link has expired!");
            return "login";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);

        model.addAttribute("message", "Password updated successfully. Please login with your new password.");
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

        Bus existingBus = busRepository.findByBusNumber(busNumber);

        if (existingBus != null) {
            model.addAttribute("message", "Bus number already exists!");
            return "driver-add-bus";
        }

        Bus bus = new Bus();
        bus.setBusNumber(busNumber);
        bus.setBusName(busName);
        bus.setBusType(busType);
        bus.setSource(source);
        bus.setDestination(destination);
        bus.setStops(stops);
        bus.setCurrentStop(source);
        bus.setCompletedStops(0);
        bus.setSpeed(0.0); // Initialize at 0 km/h before engine dispatch
        bus.setDistanceRemaining(12.5);

        // Tamil Nadu Central Focus Layout Default Coords
        bus.setLatitude(11.1271);
        bus.setLongitude(78.6569);

        if (authentication != null) {
            bus.setDriverUsername(authentication.getName());
        }

        bus.setLastUpdated(LocalDateTime.now());
        busRepository.save(bus);

        model.addAttribute("message", "Bus details added successfully!");
        return "driver-add-bus";
    }

    @GetMapping("/track-bus")
    public String trackBus(@RequestParam String busNumber,
                           @RequestParam String busName,
                           Model model) {

        Bus bus = busRepository.findByBusNumberAndBusName(busNumber, busName);

        if (bus == null) {
            model.addAttribute("error", "Bus not found");
            return "track-bus";
        }

        model.addAttribute("bus", bus);
        return "track-bus";
    }
      @GetMapping("/api/places")
    @ResponseBody
    public List<MapPlace> getAllPlaces() {
        return mapPlaceRepository.findAll();
    }

    @PostMapping("/api/places")
    @ResponseBody
    public MapPlace savePlace(@RequestBody MapPlace place) {
        return mapPlaceRepository.save(place);
    }

   @PutMapping("/api/places/{id}")
    @ResponseBody
    public ResponseEntity<MapPlace> updatePlace(@PathVariable String id, @RequestBody MapPlace updatedDetails) {
        return mapPlaceRepository.findById(id)
                .map(place -> {
                    place.setName(updatedDetails.getName());
                    place.setLatitude(updatedDetails.getLatitude());
                    place.setLongitude(updatedDetails.getLongitude());
                    return ResponseEntity.ok(mapPlaceRepository.save(place));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/places/{id}")
    @ResponseBody
    public ResponseEntity<Void> deletePlace(@PathVariable String id) {
        if (mapPlaceRepository.existsById(id)) {
            mapPlaceRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
