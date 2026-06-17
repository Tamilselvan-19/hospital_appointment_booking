package com.hospital.booking.security;

import com.hospital.booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired private UserRepository userRepository;

    @Override @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return UserDetailsImpl.build(
            userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email))
        );
    }
}
