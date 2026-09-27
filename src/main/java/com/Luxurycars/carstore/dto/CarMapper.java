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
                // Build the image URL (CDN/external URL or local API endpoint), or null if no image
                .imageUrl(resolveImageUrl(car))
                .imageName(car.getImageName())
                .hasImage(resolveHasImage(car))
                .createdAt(car.getCreatedAt())
                .updatedAt(car.getUpdatedAt())
                .build();
    }

    private static String resolveImageUrl(Car car) {
        if (car.getImageUrl() != null && !car.getImageUrl().isBlank()) {
            return car.getImageUrl();
        }
        if (car.getImagePath() != null && !car.getImagePath().isBlank()) {
            return "/api/cars/" + car.getId() + "/image";
        }
        return null;
    }

    private static boolean resolveHasImage(Car car) {
        return (car.getImageUrl() != null && !car.getImageUrl().isBlank())
                || (car.getImagePath() != null && !car.getImagePath().isBlank());
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
