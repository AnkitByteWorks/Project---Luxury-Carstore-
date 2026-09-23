package com.Luxurycars.carstore.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // omit null fields in JSON
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;              // HTTP status code: 404, 400, 500...
    private String error;            // short label: "Not Found", "Bad Request"
    private String message;          // human-readable message
    private String path;             // which URL caused this
    private Map<String, String> fieldErrors; // validation errors per field (only if applicable)
}