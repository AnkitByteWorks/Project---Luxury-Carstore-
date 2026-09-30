package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.TestDriveRequestDTO;
import com.Luxurycars.carstore.dto.TestDriveResponseDTO;
import com.Luxurycars.carstore.dto.TestDriveTrackingDTO;
import com.Luxurycars.carstore.entity.Car;
import com.Luxurycars.carstore.entity.TestDrive;
import com.Luxurycars.carstore.entity.TestDriveStatus;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.mongodb.document.CustomerAuditLog;
import com.Luxurycars.carstore.mongodb.repository.CustomerAuditLogRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TestDriveService {

    private static final Logger log = LoggerFactory.getLogger(TestDriveService.class);

    private final TestDriveRepository testDriveRepository;
    private final CarRepository carRepository;
    private final EmailNotificationService emailNotificationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final TelegramNotificationService telegramNotificationService;
    private final CustomerAuditLogRepository customerAuditLogRepository;

    @Autowired
    public TestDriveService(TestDriveRepository testDriveRepository,
                            CarRepository carRepository,
                            @Autowired(required = false) EmailNotificationService emailNotificationService,
                            @Autowired(required = false) StringRedisTemplate stringRedisTemplate,
                            @Autowired(required = false) TelegramNotificationService telegramNotificationService,
                            @Autowired(required = false) CustomerAuditLogRepository customerAuditLogRepository) {
        this.testDriveRepository = testDriveRepository;
        this.carRepository = carRepository;
        this.emailNotificationService = emailNotificationService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.telegramNotificationService = telegramNotificationService;
        this.customerAuditLogRepository = customerAuditLogRepository;
    }

    public TestDriveService(TestDriveRepository testDriveRepository,
                            CarRepository carRepository,
                            EmailNotificationService emailNotificationService,
                            StringRedisTemplate stringRedisTemplate) {
        this(testDriveRepository, carRepository, emailNotificationService, stringRedisTemplate, null, null);
    }

    public TestDriveService(TestDriveRepository testDriveRepository,
                            CarRepository carRepository,
                            EmailNotificationService emailNotificationService) {
        this(testDriveRepository, carRepository, emailNotificationService, null, null, null);
    }

    public TestDriveService(TestDriveRepository testDriveRepository, CarRepository carRepository) {
        this(testDriveRepository, carRepository, null, null, null, null);
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

        if (telegramNotificationService != null) {
            telegramNotificationService.sendTestDriveAlert(responseDTO);
        }

        if (customerAuditLogRepository != null) {
            try {
                CustomerAuditLog audit = CustomerAuditLog.builder()
                        .customerIdentifier(dto.getEmail())
                        .customerName(dto.getCustomerName())
                        .actionType("VIP_TEST_DRIVE_BOOKED")
                        .carId(car.getId())
                        .carModel(car.getName())
                        .eventMetadata(Map.of(
                                "referenceCode", referenceCode,
                                "timeSlot", dto.getTimeSlot(),
                                "preferredDate", dto.getPreferredDate().toString(),
                                "experienceType", dto.getExperienceType().toString(),
                                "phone", dto.getPhone()
                        ))
                        .timestamp(LocalDateTime.now())
                        .build();
                customerAuditLogRepository.save(audit);
                log.info("Persisted customer audit log in MongoDB for ref: {}", referenceCode);
            } catch (Exception ex) {
                log.warn("Non-blocking MongoDB audit log failure: {}", ex.getMessage());
            }
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

        if (customerAuditLogRepository != null) {
            try {
                CustomerAuditLog audit = CustomerAuditLog.builder()
                        .customerIdentifier(td.getEmail())
                        .customerName(td.getCustomerName())
                        .actionType("TEST_DRIVE_STATUS_TRANSITION")
                        .carId(td.getCarId())
                        .eventMetadata(Map.of(
                                "referenceCode", td.getReferenceCode(),
                                "newStatus", status.toString()
                        ))
                        .timestamp(LocalDateTime.now())
                        .build();
                customerAuditLogRepository.save(audit);
            } catch (Exception ex) {
                log.warn("Non-blocking MongoDB audit log failure on status update: {}", ex.getMessage());
            }
        }

        return toResponseDTOWithCarLookup(updated);
    }

    @Transactional(readOnly = true)
    public TestDriveTrackingDTO getTrackingTimeline(String referenceCode) {
        TestDrive td = testDriveRepository.findByReferenceCode(referenceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Test drive not found with reference code: " + referenceCode));
        Car car = carRepository.findById(td.getCarId()).orElse(null);

        TestDriveStatus status = td.getStatus();
        int currentStep = 1;
        if (status == TestDriveStatus.CONCIERGE_ASSIGNED || status == TestDriveStatus.CONFIRMED) {
            currentStep = 2;
        } else if (status == TestDriveStatus.CARRIER_DISPATCHED) {
            currentStep = 3;
        } else if (status == TestDriveStatus.COMPLETED) {
            currentStep = 5;
        }

        List<TestDriveTrackingDTO.TrackingStep> steps = new ArrayList<>();
        steps.add(TestDriveTrackingDTO.TrackingStep.builder()
                .stepNumber(1)
                .title("VIP Reservation Received")
                .description("Your confidential test drive booking has been recorded in the Carstore registry.")
                .completed(true)
                .active(currentStep == 1)
                .timestamp(td.getCreatedAt())
                .build());

        steps.add(TestDriveTrackingDTO.TrackingStep.builder()
                .stepNumber(2)
                .title("Master Concierge Assigned")
                .description("Dedicated VIP Host assigned to verify credentials, route clearance, and safety telemetry.")
                .completed(currentStep >= 2)
                .active(currentStep == 2)
                .timestamp(currentStep >= 2 ? td.getCreatedAt().plusMinutes(15) : null)
                .build());

        steps.add(TestDriveTrackingDTO.TrackingStep.builder()
                .stepNumber(3)
                .title("White-Glove Enclosed Carrier Loaded")
                .description("Hypercar loaded into climate-controlled hydraulic transporter with air-ride suspension.")
                .completed(currentStep >= 3)
                .active(currentStep == 3)
                .timestamp(currentStep >= 3 ? td.getCreatedAt().plusHours(1) : null)
                .build());

        steps.add(TestDriveTrackingDTO.TrackingStep.builder()
                .stepNumber(4)
                .title("En Route to Location / Circuit")
                .description("Transporter in transit with Level-3 security escort and real-time telemetry streaming.")
                .completed(currentStep >= 4)
                .active(currentStep == 4)
                .timestamp(currentStep >= 4 ? td.getCreatedAt().plusHours(2) : null)
                .build());

        steps.add(TestDriveTrackingDTO.TrackingStep.builder()
                .stepNumber(5)
                .title("Handover & Private Track Session")
                .description("Keys presented in bespoke milled-aluminum presentation case. Drive completed.")
                .completed(currentStep == 5)
                .active(currentStep == 5)
                .timestamp(currentStep == 5 ? td.getCreatedAt().plusHours(3) : null)
                .build());

        TestDriveTrackingDTO.ConciergeInfo concierge = TestDriveTrackingDTO.ConciergeInfo.builder()
                .name("Armaan Singhania")
                .title("Director of Private Client Relations")
                .phone("+91 98200 11223")
                .badge("Level-5 Diplomatic Concierge")
                .build();

        TestDriveTrackingDTO.LogisticsInfo logistics = TestDriveTrackingDTO.LogisticsInfo.builder()
                .carrierId("CARRIER-VIP-09")
                .transporterType("Enclosed Twin-Axle Aerodynamic Pod")
                .driverName("Vikram Rathore (Armed Escort Lead)")
                .currentCheckpoint(currentStep >= 3 ? "NH-48 Corridor • Approaching Destination Perimeter" : "Staged at Central Vault Hub")
                .climateControlTemp("20.8°C Regulated")
                .estimatedArrival(td.getPreferredDate() + " • " + td.getTimeSlot())
                .build();

        return TestDriveTrackingDTO.builder()
                .referenceCode(td.getReferenceCode())
                .carId(td.getCarId())
                .carName(car != null ? car.getName() : "Unknown Hypercar")
                .carBrand(car != null ? car.getBrand() : "Carstore Atelier")
                .customerName(td.getCustomerName())
                .preferredDate(td.getPreferredDate())
                .timeSlot(td.getTimeSlot())
                .experienceType(td.getExperienceType() != null ? td.getExperienceType().toString() : "VIP Track Session")
                .currentStatus(td.getStatus())
                .currentStep(currentStep)
                .steps(steps)
                .concierge(concierge)
                .logistics(logistics)
                .build();
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
