package com.hospital.booking.dto;
import com.hospital.booking.entity.User;
import lombok.*;
import java.time.LocalDateTime;

/** BUG FIX: Admin endpoint was returning raw User entity with password hash included.
    This safe DTO omits the password field entirely. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private User.Role role;
    private Boolean isActive;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public static UserDTO from(User u) {
        return UserDTO.builder()
            .id(u.getId()).name(u.getName()).email(u.getEmail())
            .phoneNumber(u.getPhoneNumber()).role(u.getRole())
            .isActive(u.getIsActive()).emailVerified(u.getEmailVerified())
            .createdAt(u.getCreatedAt()).lastLogin(u.getLastLogin()).build();
    }
}
