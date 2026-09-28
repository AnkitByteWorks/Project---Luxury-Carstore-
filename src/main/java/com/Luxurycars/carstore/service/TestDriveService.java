package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.TestDriveRequestDTO;
import com.Luxurycars.carstore.dto.TestDriveResponseDTO;
import com.Luxurycars.carstore.entity.Car;
import com.Luxurycars.carstore.entity.TestDrive;
import com.Luxurycars.carstore.entity.TestDriveStatus;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.repository.CarRepository;
import com.Luxurycars.carstore.repository.TestDriveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TestDriveService {

    private static final Logger log = LoggerFactory.getLogger(TestDriveService.class);

    private final TestDriveRepository testDriveRepository;
    private final CarRepository carRepository;
    private final EmailNotificationService emailNotificationService;
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public TestDriveService(TestDriveRepository testDriveRepository,
                            CarRepository carRepository,
                            @Autowired(required = false) EmailNotificationService emailNotificationService,
                            @Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
        this.testDriveRepository = testDriveRepository;
        this.carRepository = carRepository;
        this.emailNotificationService = emailNotificationService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public TestDriveService(TestDriveRepository testDriveRepository,
                            CarRepository carRepository,
                            EmailNotificationService emailNotificationService) {
        this(testDriveRepository, carRepository, emailNotificationService, null);
    }

    public TestDriveService(TestDriveRepository testDriveRepository, CarRepository carRepository) {
        this(testDriveRepository, carRepository, null, null);
    }

    @Transactional
    public TestDriveResponseDTO createTestDrive(TestDriveRequestDTO dto) {
        log.info("Booking VIP test drive for car ID: {} by customer: {}", dto.getCarId(), dto.getCustomerName());

        if (stringRedisTemplate != null) {
            String lockKey = "lock:testdrive:" + dto.getCarId() + ":" + dto.getPreferredDate() + ":" + dto.getTimeSlot();
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofMinutes(10));
            if (Boolean.FALSE.equals(acquired)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This VIP slot is already reserved or currently locked under high demand.");
            }
        }

        Car car = carRepository.findById(dto.getCarId())
                .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + dto.getCarId()));

        String referenceCode = "TD-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        TestDrive testDrive = TestDrive.builder()
                .carId(car.getId())
                .customerName(dto.getCustomerName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .preferredDate(dto.getPreferredDate())
                .timeSlot(dto.getTimeSlot())
                .experienceType(dto.getExperienceType())
                .referenceCode(referenceCode)
                .status(TestDriveStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        TestDrive saved = testDriveRepository.save(testDrive);
        log.info("VIP test drive booked successfully with reference: {}", referenceCode);

        TestDriveResponseDTO responseDTO = toResponseDTO(saved, car);

        if (emailNotificationService != null) {
            emailNotificationService.sendTestDriveConfirmationEmail(responseDTO);
        }

        return responseDTO;
    }

    @Transactional(readOnly = true)
    public List<TestDriveResponseDTO> getAllTestDrives() {
        return testDriveRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponseDTOWithCarLookup)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TestDriveResponseDTO getByReferenceCode(String referenceCode) {
        TestDrive td = testDriveRepository.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Test drive not found with reference code: " + referenceCode));
        return toResponseDTOWithCarLookup(td);
    }

    @Transactional
    public TestDriveResponseDTO updateStatus(Long id, TestDriveStatus status) {
        log.info("Updating test drive ID {} status to: {}", id, status);
        TestDrive td = testDriveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test drive not found with id: " + id));

        td.setStatus(status);
        TestDrive updated = testDriveRepository.save(td);
        return toResponseDTOWithCarLookup(updated);
    }

    private TestDriveResponseDTO toResponseDTOWithCarLookup(TestDrive td) {
        Car car = carRepository.findById(td.getCarId()).orElse(null);
        return toResponseDTO(td, car);
    }

    private TestDriveResponseDTO toResponseDTO(TestDrive td, Car car) {
        return TestDriveResponseDTO.builder()
                .id(td.getId())
                .carId(td.getCarId())
                .carName(car != null ? car.getName() : "Unknown Car")
                .carBrand(car != null ? car.getBrand() : "Unknown Brand")
                .customerName(td.getCustomerName())
                .phone(td.getPhone())
                .email(td.getEmail())
                .preferredDate(td.getPreferredDate())
                .timeSlot(td.getTimeSlot())
                .experienceType(td.getExperienceType())
                .referenceCode(td.getReferenceCode())
                .status(td.getStatus())
                .createdAt(td.getCreatedAt())
                .build();
    }
}
