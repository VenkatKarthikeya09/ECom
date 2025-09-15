package com.example.ecom.repository;

import com.example.ecom.model.Orders;
import com.example.ecom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdersRepository extends JpaRepository<Orders, Integer> {
	List<Orders> findByUser(User user);
	List<Orders> findByAddressEmail(String email);
} 