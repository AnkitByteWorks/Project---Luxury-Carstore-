package com.Luxurycars.carstore.dto;



import com.Luxurycars.carstore.entity.Car;

public class CarMapper {

    // ─── Entity → ResponseDTO ───
    public static CarResponseDTO toResponseDTO(Car car) {
        return CarResponseDTO.builder()
                .id(car.getId())
                .name(car.getName())
                .brand(car.getBrand())
                .price(car.getPrice())
                .description(car.getDescription())
                .colorOptions(car.getColorOptions())
                .showroomLocation(car.getShowroomLocation())
                .deliveryDays(car.getDeliveryDays())
                .paymentOptions(car.getPaymentOptions())
                // Build the image URL, or null if no image
                .imageUrl(car.getImagePath() != null ? "/api/cars/" + car.getId() + "/image" : null)
                .imageName(car.getImageName())
                .hasImage(car.getImagePath() != null)
                .createdAt(car.getCreatedAt())
                .updatedAt(car.getUpdatedAt())
                .build();
    }

    // ─── RequestDTO → Entity (for POST/PUT) ───
    public static Car toEntity(CarRequestDTO dto) {
        return Car.builder()
                .name(dto.getName())
                .brand(dto.getBrand())
                .price(dto.getPrice())
                .description(dto.getDescription())
                .colorOptions(dto.getColorOptions())
                .showroomLocation(dto.getShowroomLocation())
                .deliveryDays(dto.getDeliveryDays())
                .paymentOptions(dto.getPaymentOptions())
                .build();
    }
}
