package com.example.ecom.repository;

import com.example.ecom.model.OrderItem;
import com.example.ecom.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
	List<OrderItem> findByOrder(Orders order);
} 