package com.hospital.booking.controller;

import com.hospital.booking.entity.User;
import com.hospital.booking.security.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class ViewController {

    // ── Public pages ──────────────────────────────────────────────────────────
    @GetMapping("/")        public String home()     { return "index"; }
    @GetMapping("/login")   public String login()    { return "login"; }
    @GetMapping("/register") public String register() { return "register"; }

    // ── Dashboard router ──────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetailsImpl u, Model m) {
        if (u == null) return "redirect:/login";
        m.addAttribute("user", u);
        return switch (u.getRole()) {
            case DOCTOR  -> "doctor/dashboard";
            case PATIENT -> "patient/dashboard";
        };
    }

    // ── Doctor ────────────────────────────────────────────────────────────────
    @GetMapping("/doctor/profile")
    public String doctorProfile(@AuthenticationPrincipal UserDetailsImpl u, Model m) {
        return doctorView(u, m, "doctor/profile");
    }

    @GetMapping("/doctor/appointments")
    public String doctorAppointments(@AuthenticationPrincipal UserDetailsImpl u, Model m) {
        return doctorView(u, m, "doctor/appointments");
    }

    @GetMapping("/doctor/schedule")
    public String doctorSchedule(@AuthenticationPrincipal UserDetailsImpl u, Model m) {
        return doctorView(u, m, "doctor/schedule");
    }

    // ── Patient ───────────────────────────────────────────────────────────────
    @GetMapping("/patient/profile")
    public String patientProfile(@AuthenticationPrincipal UserDetailsImpl u, Model m) {
        return patientView(u, m, "patient/profile");
    }

    @GetMapping("/patient/book-appointment")
    public String bookAppointment(@AuthenticationPrincipal UserDetailsImpl u, Model m) {
        return patientView(u, m, "patient/book-appointment");
    }

    // Both URL variants map to the same template
    @GetMapping("/patient/appointments")
    public String patientAppointments(@AuthenticationPrincipal UserDetailsImpl u, Model m) {
        return patientView(u, m, "patient/my-appointments");
    }

    @GetMapping("/patient/my-appointments")
    public String myAppointments(@AuthenticationPrincipal UserDetailsImpl u, Model m) {
        return patientView(u, m, "patient/my-appointments");
    }

    // Both URL variants map to the same template
    @GetMapping("/patient/doctors")
    public String patientDoctors(@AuthenticationPrincipal UserDetailsImpl u, Model m) {
        return patientView(u, m, "patient/doctors");
    }

    @GetMapping("/patient/find-doctors")
    public String findDoctors(@AuthenticationPrincipal UserDetailsImpl u, Model m) {
        return patientView(u, m, "patient/doctors");
    }

    // ── Access-denied page ────────────────────────────────────────────────────
    @GetMapping("/access-denied")
    public String accessDenied(Model m) {
        m.addAttribute("status", 403);
        m.addAttribute("error", "Forbidden");
        m.addAttribute("message", "You do not have permission to access this page.");
        return "error";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String doctorView(UserDetailsImpl u, Model m, String view) {
        if (u == null) return "redirect:/login";
        if (u.getRole() != User.Role.DOCTOR) return "redirect:/dashboard";
        m.addAttribute("user", u);
        return view;
    }

    private String patientView(UserDetailsImpl u, Model m, String view) {
        if (u == null) return "redirect:/login";
        if (u.getRole() != User.Role.PATIENT) return "redirect:/dashboard";
        m.addAttribute("user", u);
        return view;
    }
}
