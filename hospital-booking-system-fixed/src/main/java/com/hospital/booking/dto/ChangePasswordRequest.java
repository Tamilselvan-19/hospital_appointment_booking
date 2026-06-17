package com.hospital.booking.dto;
import jakarta.validation.constraints.*;
import lombok.*;

/** BUG FIX: changePassword was @RequestParam userId — exposed on URL, anyone could reset others password.
    Now uses a proper request body with auth from JWT principal. */
@Data @NoArgsConstructor @AllArgsConstructor
public class ChangePasswordRequest {
    @NotBlank private String oldPassword;
    @NotBlank @Size(min=6) private String newPassword;
}
