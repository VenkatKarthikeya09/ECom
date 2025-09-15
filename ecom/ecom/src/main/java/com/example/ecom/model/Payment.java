package com.example.ecom.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "Payment")
public class Payment {
	public enum Method { CARD, UPI_QR, COD }
	public enum Status { PENDING, SUBMITTED, PAID, FAILED }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer paymentId;

	@OneToOne
	@JoinColumn(name = "orderId", nullable = false)
	private Orders order;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Method method;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.PENDING;

	// Card
	private String cardHolder;
	private String cardLast4;
	private Integer cardExpMonth;
	private Integer cardExpYear;

	// UPI
	private String upiId;

	// Verification
	private String referenceNumber;
	private String bankName;
	private String payerName;

	@Column(nullable = false)
	private double amount;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	// getters/setters
	public Integer getPaymentId() { return paymentId; }
	public void setPaymentId(Integer paymentId) { this.paymentId = paymentId; }
	public Orders getOrder() { return order; }
	public void setOrder(Orders order) { this.order = order; }
	public Method getMethod() { return method; }
	public void setMethod(Method method) { this.method = method; }
	public Status getStatus() { return status; }
	public void setStatus(Status status) { this.status = status; }
	public String getCardHolder() { return cardHolder; }
	public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }
	public String getCardLast4() { return cardLast4; }
	public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
	public Integer getCardExpMonth() { return cardExpMonth; }
	public void setCardExpMonth(Integer cardExpMonth) { this.cardExpMonth = cardExpMonth; }
	public Integer getCardExpYear() { return cardExpYear; }
	public void setCardExpYear(Integer cardExpYear) { this.cardExpYear = cardExpYear; }
	public String getUpiId() { return upiId; }
	public void setUpiId(String upiId) { this.upiId = upiId; }
	public String getReferenceNumber() { return referenceNumber; }
	public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
	public String getBankName() { return bankName; }
	public void setBankName(String bankName) { this.bankName = bankName; }
	public String getPayerName() { return payerName; }
	public void setPayerName(String payerName) { this.payerName = payerName; }
	public double getAmount() { return amount; }
	public void setAmount(double amount) { this.amount = amount; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
} 