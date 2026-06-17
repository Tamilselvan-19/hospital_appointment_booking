package com.hospital.booking.repository;
import com.hospital.booking.entity.Payment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByIdempotencyKey(String key);
    List<Payment> findByUserId(Long userId);
    List<Payment> findByStatus(Payment.Status status);
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status='SUCCESS'") Double getTotalRevenue();
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status='SUCCESS' AND p.createdAt BETWEEN :s AND :e") Double getRevenueInRange(@Param("s") LocalDateTime s, @Param("e") LocalDateTime e);
    @Query("SELECT p.paymentType, SUM(p.amount) FROM Payment p WHERE p.status='SUCCESS' GROUP BY p.paymentType") List<Object[]> getRevenueByType();
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status=:s") Long countByStatus(@Param("s") Payment.Status s);
}
