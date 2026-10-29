package com.lorryowner.backend.dto.driver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDriverRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        String phoneNumber,
        String licenseNumber,
        String assignedLorryRegistration,
        Long assignedLorryId
) {
    public UpdateDriverRequest(String fullName, String phoneNumber, String licenseNumber) {
        this(fullName, phoneNumber, licenseNumber, null, null);
    }
}
