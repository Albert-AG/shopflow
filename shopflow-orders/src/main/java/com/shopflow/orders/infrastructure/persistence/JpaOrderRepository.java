package com.shopflow.orders.infrastructure.persistence;

import com.shopflow.orders.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByCustomerId(UUID customerId, Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status")
    Page<OrderEntity> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.customerId = :customerId AND o.status = 'PENDING'")
    long countPendingByCustomer(@Param("customerId") UUID customerId);
}
