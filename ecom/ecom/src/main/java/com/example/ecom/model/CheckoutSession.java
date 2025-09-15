package com.example.ecom.model;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Component
@SessionScope
public class CheckoutSession {
    public static class Address {
        private String fullName;
        private String addressLine1;
        private String addressLine2;
        private String landmark;
        private String area;
        private String city;
        private String state;
        private String postalCode;
        private String country = "India";
        private String email;
        private String phone;
        private boolean emailSameAsAccount;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
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
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public boolean isEmailSameAsAccount() { return emailSameAsAccount; }
        public void setEmailSameAsAccount(boolean emailSameAsAccount) { this.emailSameAsAccount = emailSameAsAccount; }
    }

    public enum PaymentMethod { CARD, UPI_QR, COD }

    public static class PaymentDetails {
        // Card
        private String cardHolder;
        private String cardNumber;
        private Integer expiryMonth; // 1..12
        private Integer expiryYear;  // YYYY
        private String cvv;
        // UPI
        private String upiId;

        public String getCardHolder() { return cardHolder; }
        public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        public Integer getExpiryMonth() { return expiryMonth; }
        public void setExpiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; }
        public Integer getExpiryYear() { return expiryYear; }
        public void setExpiryYear(Integer expiryYear) { this.expiryYear = expiryYear; }
        public String getCvv() { return cvv; }
        public void setCvv(String cvv) { this.cvv = cvv; }
        public String getUpiId() { return upiId; }
        public void setUpiId(String upiId) { this.upiId = upiId; }

        public boolean isCardExpiryValid() {
            if (expiryMonth == null || expiryYear == null) return false;
            try {
                java.time.YearMonth exp = java.time.YearMonth.of(expiryYear, expiryMonth);
                return exp.isAfter(java.time.YearMonth.now());
            } catch (Exception e) { return false; }
        }
    }

    public static class PaymentVerification {
        private String referenceNumber;
        private String bankName;
        private String payerName;
        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
        public String getBankName() { return bankName; }
        public void setBankName(String bankName) { this.bankName = bankName; }
        public String getPayerName() { return payerName; }
        public void setPayerName(String payerName) { this.payerName = payerName; }
    }

    private Address address = new Address();
    private PaymentMethod paymentMethod;
    private PaymentDetails paymentDetails = new PaymentDetails();
    private PaymentVerification verification = new PaymentVerification();

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public PaymentDetails getPaymentDetails() { return paymentDetails; }
    public void setPaymentDetails(PaymentDetails paymentDetails) { this.paymentDetails = paymentDetails; }
    public PaymentVerification getVerification() { return verification; }
    public void setVerification(PaymentVerification verification) { this.verification = verification; }

    public static class ReviewItem {
        private Long productId;
        private String name;
        private double price;
        private int quantity;
        private String imageUrl;
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    public List<ReviewItem> buildReviewItems(List<CartItem> cartItems) {
        List<ReviewItem> list = new ArrayList<>();
        for (CartItem ci : cartItems) {
            ReviewItem ri = new ReviewItem();
            ri.setProductId(ci.getProduct().getProductId());
            ri.setName(ci.getProduct().getName());
            ri.setPrice(ci.getProduct().getPrice());
            ri.setQuantity(ci.getQuantity());
            ri.setImageUrl(ci.getProduct().getImageUrl());
            list.add(ri);
        }
        return list;
    }
} 