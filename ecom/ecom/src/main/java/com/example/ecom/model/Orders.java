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

	// Computed reference for tracking (not persisted)
	@Transient
	private String orderRef;

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

	@PrePersist
	public void prePersist() {
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = Instant.now();
	}

	public String getOrderRef() {
		if (orderRef == null) {
			// Build deterministic human-friendly ref from id and timestamp
			String prefix = "ORD-";
			String letters = "ABCDEFGHJKLMNPQRSTUVWXYZ";
			int idPart = (orderId != null ? orderId : 0);
			long epochPart = (createdAt != null ? createdAt.getEpochSecond() : 0L);
			int hash = Math.abs((int)(idPart * 1315423911L ^ epochPart));
			char a = letters.charAt(hash % letters.length());
			char b = letters.charAt((hash / 7) % letters.length());
			char c = letters.charAt((hash / 13) % letters.length());
			long num = Math.abs((epochPart + idPart * 10007L) % 1_000_000L);
			orderRef = prefix + a + b + c + "-" + String.format("%06d", num);
		}
		return orderRef;
	}
} 