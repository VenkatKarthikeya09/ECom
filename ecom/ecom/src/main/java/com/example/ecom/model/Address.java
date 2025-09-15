package com.example.ecom.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "Address")
public class Address {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer addressId;

	@ManyToOne
	@JoinColumn(name = "userId")
	private User user; // nullable for guest orders

	@Column(length = 50)
	private String label; // e.g., Home, Work

	@Column(nullable = false, length = 100)
	private String fullName;

	@Column(nullable = false, length = 15)
	private String phone;

	@Column(nullable = false, length = 200)
	private String addressLine1;

	@Column(length = 200)
	private String addressLine2;

	@Column(length = 120)
	private String landmark;

	@Column(nullable = false, length = 100)
	private String area;

	@Column(nullable = false, length = 100)
	private String city;

	@Column(nullable = false, length = 100)
	private String state;

	@Column(nullable = false, length = 10)
	private String postalCode;

	@Column(nullable = false, length = 50)
	private String country = "India";

	@Column(nullable = false, length = 100)
	private String email;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	public Integer getAddressId() { return addressId; }
	public void setAddressId(Integer addressId) { this.addressId = addressId; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }
	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }
	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }
	public String getAddressLine1() { return addressLine1; }
	public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
	public String getAddressLine2() { return addressLine2; }
	public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
	public String getLandmark() { return landmark; }
	public void setLandmark(String landmark) { this.landmark = landmark; }
	public String getArea() { return area; }
	public void setArea(String area) { this.area = area; }
	public String getCity() { return city; }
	public void setCity(String city) { this.city = city; }
	public String getState() { return state; }
	public void setState(String state) { this.state = state; }
	public String getPostalCode() { return postalCode; }
	public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
	public String getCountry() { return country; }
	public void setCountry(String country) { this.country = country; }
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
} 