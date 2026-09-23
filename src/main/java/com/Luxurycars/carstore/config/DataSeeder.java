package com.Luxurycars.carstore.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.Luxurycars.carstore.entity.AppUser;
import com.Luxurycars.carstore.entity.Role;
import com.Luxurycars.carstore.repository.UserRepository;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Set;

import com.Luxurycars.carstore.entity.Car;
import com.Luxurycars.carstore.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(2) // ← Spring creates this bean at startup

public class DataSeeder implements CommandLineRunner {
    // ↑ CommandLineRunner: Spring runs its run() method ONCE after startup
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataSeeder(CarRepository carRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Skip if DB already has cars (so we don't duplicate on every restart)
        // ─── Seed an admin user ───
        if (!userRepository.existsByUsername("admin")) {
            AppUser admin = AppUser.builder()
                    .username("admin")
                    .email("admin@luxurycars.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Admin")
                    .roles(Set.of(Role.ADMIN, Role.USER))
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("✅ Created default admin: username=admin, password=admin123");
        }
        if (carRepository.count() > 0) {
            log.info("✅ Cars already exist. Skipping seeding.");
            return;
        }

        log.info("🚗 Seeding 20 luxury cars...");

        List<Car> cars = List.of(
                car("Porsche 911 Turbo S", "Porsche", 28500000.00,
                        "Iconic German sports car with a 650 HP twin-turbo flat-six. 0–100 km/h in 2.7s.",
                        "GT Silver, Guards Red, Racing Yellow, Jet Black",
                        "Mumbai", 45, "Cash, Bank Transfer, EMI, Crypto"),

                car("Ferrari SF90 Stradale", "Ferrari", 75000000.00,
                        "Hybrid V8 + 3 electric motors producing a combined 986 HP. Maranello's finest.",
                        "Rosso Corsa, Nero Daytona, Argento Nurburgring",
                        "Delhi", 90, "Bank Transfer, Crypto"),

                car("Lamborghini Aventador SVJ", "Lamborghini", 82500000.00,
                        "Naturally aspirated V12, 759 HP, screaming to 8500 RPM. Pure drama.",
                        "Arancio Argos, Verde Mantis, Grigio Titans",
                        "Bangalore", 120, "Bank Transfer, Crypto"),

                car("McLaren 720S", "McLaren", 52000000.00,
                        "Carbon-fiber monocoque, 710 HP, and physics-defying aerodynamics.",
                        "Papaya Spark, Volcano Yellow, Onyx Black",
                        "Mumbai", 60, "Bank Transfer, EMI"),

                car("Bugatti Chiron Super Sport", "Bugatti", 400000000.00,
                        "W16 quad-turbo, 1578 HP, and a 440 km/h top speed. Hypercar royalty.",
                        "Deep Blue, Italian Red, Silk Silver",
                        "Delhi", 180, "Bank Transfer, Crypto"),

                car("Rolls-Royce Phantom", "Rolls-Royce", 95000000.00,
                        "The pinnacle of luxury. Whisper-quiet V12 and a starlight headliner.",
                        "English White, Diamond Black, Arctic White",
                        "Mumbai", 150, "Bank Transfer, EMI"),

                car("Bentley Continental GT", "Bentley", 42000000.00,
                        "Handcrafted British grand tourer with a 6.0L twin-turbo W12.",
                        "Beluga Black, Moonbeam, Verdant Green",
                        "Hyderabad", 75, "Cash, EMI, Bank Transfer"),

                car("Aston Martin DBS Superleggera", "Aston Martin", 48000000.00,
                        "5.2L twin-turbo V12, 715 HP, and the most beautiful body in the business.",
                        "Hyper Red, Satin Xenon Grey, Onyx Black",
                        "Delhi", 90, "Bank Transfer, EMI"),

                car("Mercedes-AMG GT Black Series", "Mercedes-AMG", 38000000.00,
                        "The most powerful AMG V8 ever: 720 HP. Track weapon.",
                        "Magmabeam, Obsidian Black, Selenite Grey",
                        "Pune", 60, "Cash, Bank Transfer, EMI, Crypto"),

                car("Audi R8 V10 Performance", "Audi", 26500000.00,
                        "The last of the NA V10s. 611 HP, quattro grip, everyday supercar.",
                        "Tango Red, Daytona Grey, Mythos Black",
                        "Chennai", 40, "Cash, EMI, Bank Transfer"),

                car("BMW M8 Competition", "BMW", 24500000.00,
                        "4.4L twin-turbo V8, 617 HP, luxurious and brutally quick.",
                        "Marina Bay Blue, Frozen Black, Isle of Man Green",
                        "Mumbai", 30, "Cash, EMI, Bank Transfer"),

                car("Tesla Model S Plaid", "Tesla", 13500000.00,
                        "Tri-motor, 1020 HP, 0–100 in 2.1s. The fastest production sedan.",
                        "Pearl White, Deep Blue, Solid Black",
                        "Bangalore", 20, "Bank Transfer, Crypto, EMI"),

                car("Nissan GT-R Nismo", "Nissan", 22500000.00,
                        "Godzilla. 600 HP, ATTESA AWD, and legendary tuning potential.",
                        "Brilliant Silver, Vibrant Red, Jet Black",
                        "Delhi", 90, "Cash, EMI"),

                car("Jaguar F-Type R", "Jaguar", 18500000.00,
                        "Supercharged V8, 575 HP, and one of the best exhaust notes ever.",
                        "Firenze Red, Santorini Black, Yulong White",
                        "Kolkata", 60, "Cash, Bank Transfer"),

                car("Maserati MC20", "Maserati", 42000000.00,
                        "Nettuno V6 with F1-derived pre-chamber combustion. 630 HP.",
                        "Bianco Audace, Nero Enigma, Blu Infinito",
                        "Mumbai", 90, "Bank Transfer, Crypto"),

                car("Lexus LC 500", "Lexus", 16500000.00,
                        "Naturally aspirated V8 and arguably the best-looking coupe on sale.",
                        "Structural Blue, Infrared, Ultra White",
                        "Chennai", 45, "Cash, EMI"),

                car("Chevrolet Corvette Z06", "Chevrolet", 15500000.00,
                        "Flat-plane crank V8, 670 HP, mid-engine American icon.",
                        "Torch Red, Rapid Blue, Arctic White",
                        "Hyderabad", 120, "Bank Transfer, EMI"),

                car("Ford Mustang Shelby GT500", "Ford", 12500000.00,
                        "Supercharged 5.2L V8, 760 HP. The most powerful Mustang ever.",
                        "Grabber Blue, Rapid Red, Shadow Black",
                        "Pune", 75, "Cash, EMI"),

                car("Porsche Taycan Turbo S", "Porsche", 24500000.00,
                        "All-electric, 750 HP, and Porsche's signature driving dynamics.",
                        "Frozen Berry, Ice Grey, Gentian Blue",
                        "Bangalore", 30, "Bank Transfer, EMI, Crypto"),

                car("Koenigsegg Jesko", "Koenigsegg", 320000000.00,
                        "5.0L twin-turbo V8, 1600 HP on E85. 500 km/h capable.",
                        "Arctic White, Karosseri Blue, Ghost",
                        "Delhi", 240, "Bank Transfer, Crypto")
        );

        carRepository.saveAll(cars);
        log.info("✅ Successfully seeded " + cars.size() + " luxury cars!");
    }

    // ─── HELPER: build a Car object quickly ───
    private Car car(String name, String brand, double price,
                    String description, String colors,
                    String location, int deliveryDays, String payments) {
        return Car.builder()
                .name(name)
                .brand(brand)
                .price(BigDecimal.valueOf(price))
                .description(description)
                .colorOptions(colors)
                .showroomLocation(location)
                .deliveryDays(deliveryDays)
                .paymentOptions(payments)
                .build();
    }
}
