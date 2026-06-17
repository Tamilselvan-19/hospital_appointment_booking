package com.hospital.booking.controller;

import com.hospital.booking.dto.*;
import com.hospital.booking.security.UserDetailsImpl;
import com.hospital.booking.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:8080","http://localhost:3000"}, maxAge = 3600)
@Slf4j
public class AuthController {

    @Autowired private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(
            @Valid @RequestBody LoginRequest req, HttpServletResponse res) {
        log.info("Login attempt: {}", req.getEmail());
        JwtResponse jwt = authService.authenticateUser(req);
        // Set HttpOnly cookie for browser navigation
        res.addHeader(HttpHeaders.SET_COOKIE, jwtCookie(jwt.getToken(), 86400));
        return ResponseEntity.ok(ApiResponse.success("Login successful", jwt));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody SignupRequest req) {
        log.info("Register: {}", req.getEmail());
        ApiResponse<String> r = authService.registerUser(req);
        return r.isSuccess() ? ResponseEntity.ok(r) : ResponseEntity.badRequest().body(r);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtResponse>> refresh(
            @RequestBody String refreshToken, HttpServletResponse res) {
        JwtResponse jwt = authService.refreshToken(refreshToken);
        res.addHeader(HttpHeaders.SET_COOKIE, jwtCookie(jwt.getToken(), 86400));
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", jwt));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse res) {
        res.addHeader(HttpHeaders.SET_COOKIE, jwtCookie("", 0));
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    /**
     * BUG FIX: Original used @RequestParam Long userId — anyone authenticated could reset
     * any other user's password by changing the userId param.
     * Now uses @AuthenticationPrincipal to get userId from the JWT token itself.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChangePasswordRequest req) {
        if (userDetails == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        ApiResponse<String> r = authService.changePassword(userDetails.getId(), req.getOldPassword(), req.getNewPassword());
        return r.isSuccess() ? ResponseEntity.ok(r) : ResponseEntity.badRequest().body(r);
    }

    private String jwtCookie(String value, long maxAge) {
        return ResponseCookie.from("jwt", value)
            .httpOnly(true).secure(false).path("/").maxAge(maxAge).sameSite("Strict").build().toString();
    }
}
