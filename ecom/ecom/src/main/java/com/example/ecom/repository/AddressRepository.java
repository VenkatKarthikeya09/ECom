package com.example.ecom.repository;

import com.example.ecom.model.Address;
import com.example.ecom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Integer> {
	List<Address> findByUser(User user);
} 