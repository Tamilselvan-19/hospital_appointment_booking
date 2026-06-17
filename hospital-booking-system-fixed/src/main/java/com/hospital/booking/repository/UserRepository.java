package com.hospital.booking.repository;
import com.hospital.booking.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    List<User> findByRole(User.Role role);
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role") Long countByRole(@Param("role") User.Role role);
    @Query("SELECT u FROM User u WHERE u.name LIKE %:kw% OR u.email LIKE %:kw%") List<User> searchUsers(@Param("kw") String kw);
}
