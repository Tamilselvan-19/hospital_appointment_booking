package com.hospital.booking.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component @Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired private JwtUtils jwtUtils;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(req);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                UserDetails ud = userDetailsService.loadUserByUsername(
                    jwtUtils.getUserNameFromJwtToken(jwt));
                var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        chain.doFilter(req, res);
    }

    private String parseJwt(HttpServletRequest req) {
        // 1. Authorization header (API clients / Postman)
        String header = req.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer "))
            return header.substring(7);
        // 2. HttpOnly cookie (browser page navigations)
        if (req.getCookies() != null)
            for (Cookie c : req.getCookies())
                if ("jwt".equals(c.getName()) && StringUtils.hasText(c.getValue()))
                    return c.getValue();
        return null;
    }
}
