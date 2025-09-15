package com.example.ecom.model;

import jakarta.persistence.*;

@Entity
@Table(name = "OrderItem")
public class OrderItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer orderItemId;

	@ManyToOne(optional = false)
	@JoinColumn(name = "orderId")
	private Orders order;

	@ManyToOne(optional = false)
	@JoinColumn(name = "productId")
	private Product product;

	@Column(nullable = false, length = 100)
	private String productName;

	@Column(nullable = false)
	private double productPrice;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false, length = 255)
	private String imageUrl;

	// getters/setters
	public Integer getOrderItemId() { return orderItemId; }
	public void setOrderItemId(Integer orderItemId) { this.orderItemId = orderItemId; }
	public Orders getOrder() { return order; }
	public void setOrder(Orders order) { this.order = order; }
	public Product getProduct() { return product; }
	public void setProduct(Product product) { this.product = product; }
	public String getProductName() { return productName; }
	public void setProductName(String productName) { this.productName = productName; }
	public double getProductPrice() { return productPrice; }
	public void setProductPrice(double productPrice) { this.productPrice = productPrice; }
	public int getQuantity() { return quantity; }
	public void setQuantity(int quantity) { this.quantity = quantity; }
	public String getImageUrl() { return imageUrl; }
	public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
} 