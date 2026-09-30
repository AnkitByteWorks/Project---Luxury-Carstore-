package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.TestDriveRequestDTO;
import com.Luxurycars.carstore.dto.TestDriveResponseDTO;
import com.Luxurycars.carstore.entity.Car;
import com.Luxurycars.carstore.entity.ExperienceType;
import com.Luxurycars.carstore.entity.TestDrive;
import com.Luxurycars.carstore.entity.TestDriveStatus;
import com.Luxurycars.carstore.exception.ResourceNotFoundException;
import com.Luxurycars.carstore.repository.CarRepository;
import com.Luxurycars.carstore.repository.TestDriveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestDriveServiceTest {

    @Mock
    private TestDriveRepository testDriveRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Mock
    private org.springframework.data.redis.core.ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TestDriveService testDriveService;

    private Car testCar;
    private TestDrive testDrive;

    @BeforeEach
    void setUp() {
        testCar = Car.builder()
                .id(1L)
                .name("Rolls-Royce Phantom")
                .brand("Rolls-Royce")
                .build();

        testDrive = TestDrive.builder()
                .id(100L)
                .carId(1L)
                .customerName("Bruce Wayne")
                .email("bruce@waynecorp.com")
                .phone("+91 9999999999")
                .preferredDate(LocalDate.now().plusDays(2))
                .timeSlot("11:00 AM - 01:00 PM")
                .experienceType(ExperienceType.DOORSTEP)
                .referenceCode("TD-2026-TESTREF1")
                .status(TestDriveStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
                .thenReturn(Boolean.TRUE);
    }

    @Test
    @DisplayName("createTestDrive - should save and return confirmed reference code")
    void createTestDrive_shouldSaveAndReturnDto() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
        when(testDriveRepository.save(any(TestDrive.class))).thenReturn(testDrive);

        TestDriveRequestDTO dto = TestDriveRequestDTO.builder()
                .carId(1L)
                .customerName("Bruce Wayne")
                .email("bruce@waynecorp.com")
                .phone("+91 9999999999")
                .preferredDate(LocalDate.now().plusDays(2))
                .timeSlot("11:00 AM - 01:00 PM")
                .experienceType(ExperienceType.DOORSTEP)
                .build();

        TestDriveResponseDTO result = testDriveService.createTestDrive(dto);

        assertThat(result).isNotNull();
        assertThat(result.getCustomerName()).isEqualTo("Bruce Wayne");
        assertThat(result.getReferenceCode()).isEqualTo("TD-2026-TESTREF1");
        assertThat(result.getStatus()).isEqualTo(TestDriveStatus.PENDING);
        assertThat(result.getCarName()).isEqualTo("Rolls-Royce Phantom");

        verify(testDriveRepository, times(1)).save(any(TestDrive.class));
    }

    @Test
    @DisplayName("createTestDrive - should throw if car does not exist")
    void createTestDrive_shouldThrowWhenCarNotFound() {
        when(carRepository.findById(999L)).thenReturn(Optional.empty());

        TestDriveRequestDTO dto = TestDriveRequestDTO.builder()
                .carId(999L)
                .customerName("Bruce Wayne")
                .email("bruce@waynecorp.com")
                .phone("+91 9999999999")
                .preferredDate(LocalDate.now().plusDays(2))
                .timeSlot("11:00 AM - 01:00 PM")
                .experienceType(ExperienceType.SHOWROOM)
                .build();

        assertThatThrownBy(() -> testDriveService.createTestDrive(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Car not found with id: 999");

        verify(testDriveRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllTestDrives - should return mapped list")
    void getAllTestDrives_shouldReturnList() {
        when(testDriveRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(testDrive));
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));

        List<TestDriveResponseDTO> list = testDriveService.getAllTestDrives();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getReferenceCode()).isEqualTo("TD-2026-TESTREF1");
    }

    @Test
    @DisplayName("updateStatus - should update status")
    void updateStatus_shouldUpdate() {
        when(testDriveRepository.findById(100L)).thenReturn(Optional.of(testDrive));
        when(testDriveRepository.save(any(TestDrive.class))).thenReturn(testDrive);
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));

        TestDriveResponseDTO updated = testDriveService.updateStatus(100L, TestDriveStatus.CONFIRMED);

        assertThat(updated.getStatus()).isEqualTo(TestDriveStatus.CONFIRMED);
        verify(testDriveRepository, times(1)).save(testDrive);
    }

    @Test
    @DisplayName("createTestDrive - should throw CONFLICT ResponseStatusException when slot is already locked in Redis")
    void createTestDrive_shouldThrowConflict_whenSlotIsLocked() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
                .thenReturn(Boolean.FALSE);

        TestDriveRequestDTO dto = TestDriveRequestDTO.builder()
                .carId(1L)
                .customerName("Bruce Wayne")
                .email("bruce@waynecorp.com")
                .phone("+91 9999999999")
                .preferredDate(LocalDate.now().plusDays(2))
                .timeSlot("11:00 AM - 01:00 PM")
                .experienceType(ExperienceType.DOORSTEP)
                .build();

        assertThatThrownBy(() -> testDriveService.createTestDrive(dto))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("This VIP slot is already reserved or currently locked under high demand.");
    }

    @Test
    @DisplayName("getTrackingTimeline - should return Amazon-style timeline steps")
    void getTrackingTimeline_shouldReturnSteps() {
        when(testDriveRepository.findByReferenceCode("TD-2026-TESTREF1")).thenReturn(Optional.of(testDrive));
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));

        com.Luxurycars.carstore.dto.TestDriveTrackingDTO timeline = testDriveService.getTrackingTimeline("TD-2026-TESTREF1");

        assertThat(timeline).isNotNull();
        assertThat(timeline.getReferenceCode()).isEqualTo("TD-2026-TESTREF1");
        assertThat(timeline.getCurrentStep()).isEqualTo(1);
        assertThat(timeline.getSteps()).hasSize(5);
        assertThat(timeline.getSteps().get(0).getTitle()).isEqualTo("VIP Reservation Received");
        assertThat(timeline.getSteps().get(0).isCompleted()).isTrue();
        assertThat(timeline.getConcierge()).isNotNull();
        assertThat(timeline.getLogistics()).isNotNull();
    }
}
