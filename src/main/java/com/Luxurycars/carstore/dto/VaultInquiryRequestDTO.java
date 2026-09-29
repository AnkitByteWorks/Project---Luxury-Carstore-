package com.Luxurycars.carstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultInquiryRequestDTO {

    @NotBlank(message = "Allocation ID is required")
    private String allocationId;

    @NotBlank(message = "Client name is required")
    private String clientName;

    @NotBlank(message = "Client phone number is required")
    private String clientPhone;
}
