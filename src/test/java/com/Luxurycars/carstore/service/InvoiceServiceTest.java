package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.entity.Car;
import com.Luxurycars.carstore.entity.Order;
import com.Luxurycars.carstore.entity.OrderStatus;
import com.Luxurycars.carstore.repository.CarRepository;
import com.Luxurycars.carstore.repository.OrderRepository;
import com.Luxurycars.carstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private Order testOrder;
    private Car testCar;

    @BeforeEach
    void setUp() {
        testCar = Car.builder()
                .id(5L)
                .name("Bugatti Chiron Super Sport")
                .brand("Bugatti")
                .price(new BigDecimal("400000000.00"))
                .showroomLocation("Delhi")
                .colorOptions("Deep Blue, Silk Silver")
                .build();

        testOrder = Order.builder()
                .id(101L)
                .carId(5L)
                .carName("Bugatti Chiron Super Sport")
                .unitPrice(new BigDecimal("400000000.00"))
                .quantity(1)
                .totalAmount(new BigDecimal("400000000.00"))
                .customerName("Ankit Singh")
                .customerEmail("ankit@luxurycars.com")
                .customerPhone("+91 9876543210")
                .deliveryAddress("Flagship Villa 7, Worli Sea Face")
                .deliveryCity("Mumbai")
                .deliveryPincode("400018")
                .paymentMethod("Bank Transfer")
                .status(OrderStatus.CONFIRMED)
                .orderedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("generateInvoicePdf - should generate PDF when user is ADMIN")
    void generateInvoicePdf_shouldGeneratePdfWhenAdmin() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(testOrder));
        when(carRepository.findById(5L)).thenReturn(Optional.of(testCar));

        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        byte[] pdfBytes = invoiceService.generateInvoicePdf(101L, adminAuth);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(100);
        // Valid PDF starts with %PDF-
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("generateInvoicePdf - should generate PDF when user is the Order owner")
    void generateInvoicePdf_shouldGeneratePdfWhenOwner() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(testOrder));
        when(carRepository.findById(5L)).thenReturn(Optional.of(testCar));

        Authentication ownerAuth = new UsernamePasswordAuthenticationToken(
                "ankit@luxurycars.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        byte[] pdfBytes = invoiceService.generateInvoicePdf(101L, ownerAuth);

        assertThat(pdfBytes).isNotNull();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("generateInvoicePdf - should throw AccessDeniedException when user is not owner nor admin")
    void generateInvoicePdf_shouldThrowWhenUnauthorized() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(testOrder));

        Authentication strangerAuth = new UsernamePasswordAuthenticationToken(
                "stranger", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(userRepository.findByUsername("stranger")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.generateInvoicePdf(101L, strangerAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized");
    }

    @Test
    @DisplayName("generateInvoicePdf - should generate PDF with Bespoke Customization Spec line item")
    void generateInvoicePdf_shouldGeneratePdfWithBespokeSpec() {
        testOrder.setCustomOptions("21\" Forged Monoblock Wheels, Carbon Ceramic Brakes");
        testOrder.setCustomPrice(new BigDecimal("2500000.00"));
        testOrder.setTotalAmount(testOrder.getUnitPrice().add(testOrder.getCustomPrice()));

        when(orderRepository.findById(101L)).thenReturn(Optional.of(testOrder));
        when(carRepository.findById(5L)).thenReturn(Optional.of(testCar));

        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        byte[] pdfBytes = invoiceService.generateInvoicePdf(101L, adminAuth);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(100);
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("generateInvoicePdf - should generate PDF with Personalized Sill Monogram line item")
    void generateInvoicePdf_shouldGeneratePdfWithPersonalizedMonogram() {
        testOrder.setMonogramText("WAYNE VIP • CHASSIS 01");
        testOrder.setMonogramColor("Cyber Blue");

        when(orderRepository.findById(101L)).thenReturn(Optional.of(testOrder));
        when(carRepository.findById(5L)).thenReturn(Optional.of(testCar));

        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        byte[] pdfBytes = invoiceService.generateInvoicePdf(101L, adminAuth);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(100);
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
    }
}
