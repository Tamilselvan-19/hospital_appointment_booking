package com.hospital.booking.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hospital.booking.entity.User;
import lombok.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String email;
    @JsonIgnore private String password;
    private User.Role role;
    private Boolean isActive;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl build(User user) {
        return UserDetailsImpl.builder()
            .id(user.getId()).name(user.getName()).email(user.getEmail())
            .password(user.getPassword()).role(user.getRole()).isActive(user.getIsActive())
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
            .build();
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return isActive != null && isActive; }
}
