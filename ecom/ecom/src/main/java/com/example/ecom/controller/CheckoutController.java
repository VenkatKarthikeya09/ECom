package com.example.ecom.controller;

import com.example.ecom.model.*;
import com.example.ecom.repository.AddressRepository;
import com.example.ecom.repository.OrderItemRepository;
import com.example.ecom.repository.OrdersRepository;
import com.example.ecom.repository.PaymentRepository;
import com.example.ecom.repository.UserRepository;
import com.example.ecom.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired private CartService cartService;
    @Autowired private CheckoutSession checkoutSession;
    @Autowired private AddressRepository addressRepository;
    @Autowired private OrdersRepository ordersRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;

    private Orders currentOrder;

    @GetMapping
    public String entry() {
        return "redirect:/checkout/address";
    }

    @GetMapping("/address")
    public String address(Model model, Authentication auth) {
        model.addAttribute("address", checkoutSession.getAddress());
        if (auth != null && auth.isAuthenticated()) {
            userRepository.findByUsername(auth.getName()).ifPresent(u -> {
                model.addAttribute("userEmail", u.getEmail());
                model.addAttribute("savedAddresses", addressRepository.findByUser(u));
            });
        }
        return "checkout/address";
    }

    @PostMapping("/address")
    public String saveAddress(@RequestParam(value = "selectedAddressId", required = false) Integer selectedAddressId,
                              @ModelAttribute("address") CheckoutSession.Address addr,
                              @RequestParam(value = "emailSameAsAccount", required = false) String sameAsAccount,
                              Authentication auth) {
        Address usedAddress;
        if (selectedAddressId != null) {
            usedAddress = addressRepository.findById(selectedAddressId).orElse(null);
            if (usedAddress == null) {
                return "redirect:/checkout/address";
            }
        } else {
            if ("on".equalsIgnoreCase(sameAsAccount) && auth != null && auth.isAuthenticated()) {
                userRepository.findByUsername(auth.getName()).ifPresent(u -> addr.setEmail(u.getEmail()));
            }
            checkoutSession.setAddress(addr);
            Address entity = new Address();
            entity.setFullName(addr.getFullName());
            entity.setPhone(addr.getPhone());
            entity.setAddressLine1(addr.getAddressLine1());
            entity.setAddressLine2(addr.getAddressLine2());
            entity.setLandmark(addr.getLandmark());
            entity.setArea(addr.getArea());
            entity.setCity(addr.getCity());
            entity.setState(addr.getState());
            entity.setPostalCode(addr.getPostalCode());
            entity.setCountry(addr.getCountry());
            entity.setEmail(addr.getEmail());
            if (auth != null && auth.isAuthenticated()) {
                userRepository.findByUsername(auth.getName()).ifPresent(entity::setUser);
            }
            usedAddress = addressRepository.save(entity);
        }

        List<CartItem> items = new ArrayList<>(cartService.getCart().values());
        double subtotal = items.stream().mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity()).sum();
        Orders order = new Orders();
        order.setAddress(usedAddress);
        if (auth != null && auth.isAuthenticated()) {
            userRepository.findByUsername(auth.getName()).ifPresent(order::setUser);
        }
        order.setSubtotal(subtotal);
        order.setShipping(0.0);
        order.setTotal(subtotal);
        currentOrder = ordersRepository.save(order);
        for (CartItem ci : items) {
            OrderItem oi = new OrderItem();
            oi.setOrder(currentOrder);
            oi.setProduct(ci.getProduct());
            oi.setProductName(ci.getProduct().getName());
            oi.setProductPrice(ci.getProduct().getPrice());
            oi.setQuantity(ci.getQuantity());
            oi.setImageUrl(ci.getProduct().getImageUrl());
            orderItemRepository.save(oi);
        }
        return "redirect:/checkout/review";
    }

    @GetMapping("/review")
    public String review(Model model) {
        List<CartItem> items = new ArrayList<>(cartService.getCart().values());
        model.addAttribute("items", items);
        model.addAttribute("totalPrice", items.stream().mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity()).sum());
        return "checkout/review";
    }

    @PostMapping("/update")
    public String updateItem(@RequestParam Long productId, @RequestParam int quantity) {
        cartService.updateProductQuantity(productId, quantity);
        // recompute order items to match cart
        if (currentOrder != null) {
            orderItemRepository.findByOrder(currentOrder).forEach(oi -> orderItemRepository.delete(oi));
            List<CartItem> items = new ArrayList<>(cartService.getCart().values());
            double subtotal = 0.0;
            for (CartItem ci : items) {
                OrderItem oi = new OrderItem();
                oi.setOrder(currentOrder);
                oi.setProduct(ci.getProduct());
                oi.setProductName(ci.getProduct().getName());
                oi.setProductPrice(ci.getProduct().getPrice());
                oi.setQuantity(ci.getQuantity());
                oi.setImageUrl(ci.getProduct().getImageUrl());
                orderItemRepository.save(oi);
                subtotal += ci.getProduct().getPrice() * ci.getQuantity();
            }
            currentOrder.setSubtotal(subtotal);
            currentOrder.setTotal(subtotal);
            ordersRepository.save(currentOrder);
        }
        return "redirect:/checkout/review";
    }

    @GetMapping("/payment")
    public String payment(Model model) {
        model.addAttribute("method", checkoutSession.getPaymentMethod());
        model.addAttribute("details", checkoutSession.getPaymentDetails());
        return "checkout/payment";
    }

    @PostMapping("/payment-method")
    public String setMethod(@RequestParam("method") CheckoutSession.PaymentMethod method) {
        checkoutSession.setPaymentMethod(method);
        if (currentOrder != null) {
            Payment payment = paymentRepository.findByOrder(currentOrder).orElseGet(() -> {
                Payment p = new Payment();
                p.setOrder(currentOrder);
                p.setAmount(currentOrder.getTotal());
                return paymentRepository.save(p);
            });
            payment.setMethod(method == CheckoutSession.PaymentMethod.CARD ? Payment.Method.CARD :
                    method == CheckoutSession.PaymentMethod.UPI_QR ? Payment.Method.UPI_QR : Payment.Method.COD);
            paymentRepository.save(payment);
        }
        return "redirect:/checkout/payment";
    }

    @PostMapping("/payment/card")
    public String saveCard(@ModelAttribute("details") CheckoutSession.PaymentDetails details, Model model) {
        checkoutSession.setPaymentMethod(CheckoutSession.PaymentMethod.CARD);
        checkoutSession.setPaymentDetails(details);
        if (!details.isCardExpiryValid()) {
            model.addAttribute("method", checkoutSession.getPaymentMethod());
            model.addAttribute("details", details);
            model.addAttribute("error", "Card expiry must be a future month");
            return "checkout/payment";
        }
        if (currentOrder != null) {
            Payment p = paymentRepository.findByOrder(currentOrder).orElseGet(() -> {
                Payment pp = new Payment();
                pp.setOrder(currentOrder);
                pp.setAmount(currentOrder.getTotal());
                return pp;
            });
            p.setMethod(Payment.Method.CARD);
            p.setCardHolder(details.getCardHolder());
            String num = details.getCardNumber();
            if (num != null && num.length() >= 4) p.setCardLast4(num.substring(num.length() - 4));
            p.setCardExpMonth(details.getExpiryMonth());
            p.setCardExpYear(details.getExpiryYear());
            p.setStatus(Payment.Status.SUBMITTED);
            paymentRepository.save(p);
        }
        return "redirect:/checkout/verify";
    }

    @PostMapping("/payment/upi")
    public String saveUpi(@ModelAttribute("details") CheckoutSession.PaymentDetails details) {
        checkoutSession.setPaymentMethod(CheckoutSession.PaymentMethod.UPI_QR);
        checkoutSession.getPaymentDetails().setUpiId(details.getUpiId());
        if (currentOrder != null) {
            Payment p = paymentRepository.findByOrder(currentOrder).orElseGet(() -> {
                Payment pp = new Payment();
                pp.setOrder(currentOrder);
                pp.setAmount(currentOrder.getTotal());
                return pp;
            });
            p.setMethod(Payment.Method.UPI_QR);
            p.setUpiId(details.getUpiId());
            p.setStatus(Payment.Status.SUBMITTED);
            paymentRepository.save(p);
        }
        return "redirect:/checkout/verify";
    }

    @PostMapping("/payment/cod")
    public String cod() {
        checkoutSession.setPaymentMethod(CheckoutSession.PaymentMethod.COD);
        if (currentOrder != null) {
            Payment p = paymentRepository.findByOrder(currentOrder).orElseGet(() -> {
                Payment pp = new Payment();
                pp.setOrder(currentOrder);
                pp.setAmount(currentOrder.getTotal());
                return pp;
            });
            p.setMethod(Payment.Method.COD);
            p.setStatus(Payment.Status.PAID);
            paymentRepository.save(p);
            currentOrder.setStatus(Orders.Status.COD_CONFIRMED);
            ordersRepository.save(currentOrder);
        }
        cartService.getCart().clear();
        return "redirect:/checkout/confirmed";
    }

    @GetMapping("/verify")
    public String verify(Model model) {
        if (checkoutSession.getPaymentMethod() == CheckoutSession.PaymentMethod.COD) {
            return "redirect:/checkout/confirmed";
        }
        model.addAttribute("verification", checkoutSession.getVerification());
        model.addAttribute("method", checkoutSession.getPaymentMethod());
        return "checkout/verify";
    }

    @PostMapping("/verify")
    public String verifySubmit(@ModelAttribute("verification") CheckoutSession.PaymentVerification verification) {
        checkoutSession.setVerification(verification);
        if (currentOrder != null) {
            Payment p = paymentRepository.findByOrder(currentOrder).orElse(null);
            if (p != null) {
                p.setReferenceNumber(verification.getReferenceNumber());
                p.setBankName(verification.getBankName());
                p.setPayerName(verification.getPayerName());
                p.setStatus(Payment.Status.PAID);
                paymentRepository.save(p);
                currentOrder.setStatus(Orders.Status.PAID);
                ordersRepository.save(currentOrder);
            }
        }
        cartService.getCart().clear();
        return "redirect:/checkout/confirmed";
    }

    @GetMapping("/confirmed")
    public String confirmed(Model model) {
        model.addAttribute("address", checkoutSession.getAddress());
        model.addAttribute("method", checkoutSession.getPaymentMethod());
        return "checkout/confirmed";
    }
} 