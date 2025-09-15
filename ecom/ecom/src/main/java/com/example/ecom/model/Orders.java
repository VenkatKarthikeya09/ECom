package com.example.ecom.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Orders")
public class Orders {
	public enum Status { CREATED, PAID, COD_CONFIRMED, CANCELLED }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer orderId;

	@ManyToOne
	@JoinColumn(name = "userId")
	private User user;

	@ManyToOne(optional = false)
	@JoinColumn(name = "addressId")
	private Address address;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.CREATED;

	@Column(nullable = false)
	private double subtotal;

	@Column(nullable = false)
	private double shipping = 0.0;

	@Column(nullable = false)
	private double total;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	// getters/setters
	public Integer getOrderId() { return orderId; }
	public void setOrderId(Integer orderId) { this.orderId = orderId; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public Address getAddress() { return address; }
	public void setAddress(Address address) { this.address = address; }
	public Status getStatus() { return status; }
	public void setStatus(Status status) { this.status = status; }
	public double getSubtotal() { return subtotal; }
	public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
	public double getShipping() { return shipping; }
	public void setShipping(double shipping) { this.shipping = shipping; }
	public double getTotal() { return total; }
	public void setTotal(double total) { this.total = total; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
	public List<OrderItem> getItems() { return items; }
	public void setItems(List<OrderItem> items) { this.items = items; }
} 