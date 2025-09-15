package com.example.ecom.repository;

import com.example.ecom.model.Payment;
import com.example.ecom.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
	Optional<Payment> findByOrder(Orders order);
} 