package com.Luxurycars.carstore.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultInquiryResponseDTO {
    private Long id;
    private String allocationId;
    private String clientName;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}
