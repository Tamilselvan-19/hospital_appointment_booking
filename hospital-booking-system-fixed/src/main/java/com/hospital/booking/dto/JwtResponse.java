package com.hospital.booking.dto;
import com.hospital.booking.entity.User;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JwtResponse {
    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private Long id;
    private String name;
    private String email;
    private User.Role role;
    private List<String> permissions;
}
