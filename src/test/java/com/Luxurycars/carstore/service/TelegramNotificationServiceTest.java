package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.TestDriveResponseDTO;
import com.Luxurycars.carstore.entity.ExperienceType;
import com.Luxurycars.carstore.entity.TestDriveStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationServiceTest {

    @InjectMocks
    private TelegramNotificationService telegramNotificationService;

    @Test
    @DisplayName("sendTestDriveAlert - executes cleanly in fallback/log mode without throwing")
    void sendTestDriveAlert_shouldExecuteCleanly() {
        TestDriveResponseDTO dto = TestDriveResponseDTO.builder()
                .referenceCode("TD-2026-VIP01")
                .customerName("Lord Sterling")
                .phone("+91 99999 88888")
                .email("sterling@mayfair.co.uk")
                .carName("Pagani Huayra R")
                .carBrand("Pagani")
                .preferredDate(LocalDate.now().plusDays(3))
                .timeSlot("14:00 - 16:00 VIP Slot")
                .experienceType(ExperienceType.TRACK)
                .status(TestDriveStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        assertThatCode(() -> telegramNotificationService.sendTestDriveAlert(dto))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendOrderAlert and sendVaultInquiryAlert - should execute cleanly")
    void sendOrderAndVaultAlerts_shouldExecuteCleanly() {
        assertThatCode(() -> telegramNotificationService.sendOrderAlert(1L, "Ankit", BigDecimal.valueOf(350000000), "Bugatti Bolide"))
                .doesNotThrowAnyException();

        assertThatCode(() -> telegramNotificationService.sendVaultInquiryAlert("Ankit", "+91 98765 43210", "Jesko Absolut"))
                .doesNotThrowAnyException();
    }
}
