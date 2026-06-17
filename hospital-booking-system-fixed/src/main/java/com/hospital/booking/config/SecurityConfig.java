package com.hospital.booking.config;

import com.hospital.booking.security.JwtAuthenticationFilter;
import com.hospital.booking.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired private UserDetailsServiceImpl userDetailsService;
    @Autowired private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:8080", "http://localhost:3000", "http://127.0.0.1:8080"));
        cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-CSRF-TOKEN", "Accept"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            /*
             * BUG FIX: This was a stateless JWT API (Authorization: Bearer <token>),
             * but CSRF protection was still enabled and only ignored for
             * /api/auth/**, /api/payments/webhook/**, and /h2-console/**.
             * Every other state-changing call (POST /api/appointments,
             * PUT /api/doctor/availability, PUT /api/appointments/{id}/cancel, etc.)
             * was rejected with 403 before reaching the controller. The frontend's
             * apiFetch() then tried to parse that 403/redirect response as JSON,
             * which surfaced in the browser as "Failed to fetch".
             * CSRF tokens are only meaningful for cookie/session based auth;
             * since this API authenticates via JWT bearer tokens (and is
             * STATELESS, see sessionManagement below), CSRF protection is
             * disabled entirely for the API.
             */
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers(
                    "/", "/index", "/login", "/register",
                    "/access-denied", "/error",
                    "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"
                ).permitAll()
                // Public API
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/payments/webhook/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Public doctor listing (patients must browse without login)
                .requestMatchers(HttpMethod.GET, "/api/doctors", "/api/doctors/**").permitAll()
                // Dashboard (role-specific redirect handled in ViewController)
                .requestMatchers("/dashboard").authenticated()
                // Doctor
                .requestMatchers("/api/doctor/**", "/doctor/**").hasRole("DOCTOR")
                // Patient
                .requestMatchers("/api/patient/**", "/patient/**").hasRole("PATIENT")
                // Appointments API
                .requestMatchers(HttpMethod.GET,    "/api/appointments/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/appointments/**").hasRole("PATIENT")
                .requestMatchers(HttpMethod.PUT,    "/api/appointments/**").hasAnyRole("DOCTOR", "PATIENT")
                .requestMatchers(HttpMethod.DELETE, "/api/appointments/**").hasRole("PATIENT")
                // Doctor slot checking (public - needed for booking page)
                .requestMatchers(HttpMethod.GET, "/api/doctors/*/available-slots").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/doctors/*/slot-available").authenticated()
                // Payments
                .requestMatchers("/api/payments/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(401);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"success\":false,\"message\":\"Authentication required. Please log in.\"}");
                    } else {
                        res.sendRedirect("/login");
                    }
                })
                .accessDeniedHandler((req, res, denyEx) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(403);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"success\":false,\"message\":\"You do not have permission to perform this action.\"}");
                    } else {
                        res.sendRedirect("/access-denied");
                    }
                })
            )
            .headers(h -> h.frameOptions(f -> f.sameOrigin()));

        return http.build();
    }
}
