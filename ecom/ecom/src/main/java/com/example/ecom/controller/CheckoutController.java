package com.example.ecom.controller;

import com.example.ecom.model.*;
import com.example.ecom.repository.AddressRepository;
import com.example.ecom.repository.OrderItemRepository;
import com.example.ecom.repository.OrdersRepository;
import com.example.ecom.repository.PaymentRepository;
import com.example.ecom.repository.UserRepository;
import com.example.ecom.service.CartService;
import com.example.ecom.service.ProductService;
import com.example.ecom.service.AdminNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Optional;

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
    @Autowired private ProductService productService;
    @Autowired private AdminNotificationService notificationService;

    private Orders currentOrder;

    @GetMapping
    public String entry() {
        return "redirect:/checkout/address";
    }

    @GetMapping("/address")
    public String address(Model model, Authentication auth) {
        model.addAttribute("address", checkoutSession.getAddress());
        model.addAttribute("serverCartSize", cartService.getCartSize());
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
                              Authentication auth,
                              RedirectAttributes ra) {
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
            entity.setLabel((addr.getLabel() != null && !addr.getLabel().isBlank()) ? addr.getLabel() : "Home");
            if (auth != null && auth.isAuthenticated()) {
                userRepository.findByUsername(auth.getName()).ifPresent(entity::setUser);
            }
            usedAddress = addressRepository.save(entity);
        }

        // Keep session address in sync when choosing a saved address
        if (selectedAddressId != null) {
            checkoutSession.copyFromEntityAddress(usedAddress);
        }

        List<CartItem> items = new ArrayList<>(cartService.getCart().values());
        // Check stock first
        for (CartItem ci : items) {
            if (ci.getProduct().getStockQuantity() < ci.getQuantity()) {
                ra.addAttribute("error", "Currently unavailable: " + ci.getProduct().getName());
                return "redirect:/cart";
            }
        }
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
            // deduct stock
            ci.getProduct().setStockQuantity(Math.max(0, ci.getProduct().getStockQuantity() - ci.getQuantity()));
            productService.save(ci.getProduct());
            if (ci.getProduct().getStockQuantity() == 0) {
                notificationService.add("STOCK", "Out of stock: " + ci.getProduct().getName(), "/admin/products?q=" + ci.getProduct().getName());
            } else if (ci.getProduct().getStockQuantity() <= 5) {
                notificationService.add("STOCK", "Low stock: " + ci.getProduct().getName() + " (" + ci.getProduct().getStockQuantity() + ")", "/admin/products?q=" + ci.getProduct().getName());
            }
            OrderItem oi = new OrderItem();
            oi.setOrder(currentOrder);
            oi.setProduct(ci.getProduct());
            oi.setProductName(ci.getProduct().getName());
            oi.setProductPrice(ci.getProduct().getPrice());
            oi.setQuantity(ci.getQuantity());
            oi.setImageUrl(ci.getProduct().getImageUrl());
            orderItemRepository.save(oi);
        }
        // store order id in session for continuity
        checkoutSession.setCurrentOrderId(currentOrder.getOrderId());
        return "redirect:/checkout/review";
    }

    @GetMapping("/review")
    public String review(Model model) {
        // Restore current order from session if needed
        if ((currentOrder == null || currentOrder.getOrderId() == null) && checkoutSession.getCurrentOrderId() != null) {
            currentOrder = ordersRepository.findById(checkoutSession.getCurrentOrderId()).orElse(null);
        }
        // Prefer session cart items; if empty, fall back to order items
        List<CartItem> cartItems = new ArrayList<>(cartService.getCart().values());
        List<CheckoutSession.ReviewItem> reviewItems = new ArrayList<>();
        double total;
        if (!cartItems.isEmpty()) {
            reviewItems = checkoutSession.buildReviewItems(cartItems);
            total = cartItems.stream().mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity()).sum();
        } else if (currentOrder != null) {
            List<OrderItem> orderItems = orderItemRepository.findByOrder(currentOrder);
            for (OrderItem it : orderItems) {
                CheckoutSession.ReviewItem ri = new CheckoutSession.ReviewItem();
                ri.setProductId(it.getProduct() != null ? it.getProduct().getProductId() : null);
                ri.setName(it.getProductName());
                ri.setPrice(it.getProductPrice());
                ri.setQuantity(it.getQuantity());
                ri.setImageUrl(it.getImageUrl());
                reviewItems.add(ri);
            }
            total = orderItems.stream().mapToDouble(i -> i.getProductPrice() * i.getQuantity()).sum();
        } else {
            total = 0.0;
        }
        model.addAttribute("reviewItems", reviewItems);
        model.addAttribute("totalPrice", total);
        return "checkout/review";
    }

    @PostMapping("/update")
    public String updateItem(@RequestParam Long productId, @RequestParam int quantity, RedirectAttributes ra) {
        // Adjust stock based on delta
        Optional<com.example.ecom.model.Product> pOpt = productService.getProductById(productId);
        if (pOpt.isPresent()) {
            com.example.ecom.model.Product p = pOpt.get();
            // find current quantity in cart
            int currentQty = cartService.getCart().getOrDefault(productId, new CartItem(p, 0)).getQuantity();
            int delta = quantity - currentQty; // positive means increasing cart
            if (delta > 0 && p.getStockQuantity() < delta) {
                ra.addAttribute("error", "Currently unavailable: " + p.getName());
                return "redirect:/checkout/review";
            }
            // reduce stock when increasing
            if (delta > 0) {
                p.setStockQuantity(p.getStockQuantity() - delta);
                productService.save(p);
            } else if (delta < 0) {
                // return stock when reducing
                p.setStockQuantity(p.getStockQuantity() + (-delta));
                productService.save(p);
            }
        }
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
        if ((currentOrder == null || currentOrder.getOrderId() == null) && checkoutSession.getCurrentOrderId() != null) {
            currentOrder = ordersRepository.findById(checkoutSession.getCurrentOrderId()).orElse(null);
        }
        double orderTotal = 0.0;
        if (currentOrder != null) {
            orderTotal = currentOrder.getTotal();
        } else {
            // fallback to cart sum
            orderTotal = cartService.getCart().values().stream()
                    .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                    .sum();
        }
        model.addAttribute("method", checkoutSession.getPaymentMethod());
        model.addAttribute("details", checkoutSession.getPaymentDetails());
        model.addAttribute("verification", checkoutSession.getVerification());
        model.addAttribute("orderTotal", orderTotal);
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
        notificationService.add("ORDER", "COD confirmed for order #" + (currentOrder != null ? currentOrder.getOrderId() : ""), "/admin/orders");
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
                String ref = verification.getReferenceNumber();
                if (ref == null || !ref.matches("\\d{12}")) {
                    throw new IllegalArgumentException("Reference number must be exactly 12 digits");
                }
                p.setReferenceNumber(ref);
                p.setBankName(verification.getBankName());
                p.setPayerName(verification.getPayerName());
                p.setStatus(Payment.Status.PAID);
                paymentRepository.save(p);
                currentOrder.setStatus(Orders.Status.PAID);
                ordersRepository.save(currentOrder);
            }
        }
        notificationService.add("ORDER", "Payment verified for order #" + (currentOrder != null ? currentOrder.getOrderId() : ""), "/admin/orders");
        cartService.getCart().clear();
        return "redirect:/checkout/confirmed";
    }

    // New consolidated payment endpoint (no intermediate redirects)
    @PostMapping("/pay")
    public String pay(@RequestParam("method") CheckoutSession.PaymentMethod method,
                      @RequestParam(value = "cardHolder", required = false) String cardHolder,
                      @RequestParam(value = "cardNumber", required = false) String cardNumber,
                      @RequestParam(value = "expiryMonth", required = false) Integer expiryMonth,
                      @RequestParam(value = "expiryYear", required = false) Integer expiryYear,
                      @RequestParam(value = "upiLocal", required = false) String upiLocal,
                      @RequestParam(value = "upiSuffix", required = false) String upiSuffix,
                      @RequestParam(value = "referenceNumber", required = false) String referenceNumber,
                      @RequestParam(value = "bankName", required = false) String bankName,
                      @RequestParam(value = "payerName", required = false) String payerName,
                      Model model) {
        checkoutSession.setPaymentMethod(method);
        if ((currentOrder == null || currentOrder.getOrderId() == null) && checkoutSession.getCurrentOrderId() != null) {
            currentOrder = ordersRepository.findById(checkoutSession.getCurrentOrderId()).orElse(null);
        }
        if (currentOrder != null) {
            Payment payment = paymentRepository.findByOrder(currentOrder).orElseGet(() -> {
                Payment p = new Payment();
                p.setOrder(currentOrder);
                p.setAmount(currentOrder.getTotal());
                return p;
            });
            if (method == CheckoutSession.PaymentMethod.CARD) {
                if (referenceNumber == null || !referenceNumber.matches("\\d{12}")) {
                    model.addAttribute("method", method);
                    model.addAttribute("orderTotal", currentOrder.getTotal());
                    model.addAttribute("error", "Reference number must be exactly 12 digits");
                    return "checkout/payment";
                }
                payment.setMethod(Payment.Method.CARD);
                payment.setCardHolder(cardHolder);
                if (cardNumber != null && cardNumber.length() >= 4) {
                    payment.setCardLast4(cardNumber.substring(cardNumber.length() - 4));
                }
                payment.setCardExpMonth(expiryMonth);
                payment.setCardExpYear(expiryYear);
                payment.setStatus(Payment.Status.PAID);
                currentOrder.setStatus(Orders.Status.PAID);
            } else if (method == CheckoutSession.PaymentMethod.UPI_QR) {
                if (referenceNumber == null || !referenceNumber.matches("\\d{12}")) {
                    model.addAttribute("method", method);
                    model.addAttribute("orderTotal", currentOrder.getTotal());
                    model.addAttribute("error", "Reference number must be exactly 12 digits");
                    return "checkout/payment";
                }
                payment.setMethod(Payment.Method.UPI_QR);
                String upi = (upiLocal != null ? upiLocal.trim() : "") + (upiSuffix != null ? upiSuffix : "");
                payment.setUpiId(upi);
                payment.setStatus(Payment.Status.PAID);
                currentOrder.setStatus(Orders.Status.PAID);
            } else { // COD
                payment.setMethod(Payment.Method.COD);
                payment.setStatus(Payment.Status.PAID);
                currentOrder.setStatus(Orders.Status.COD_CONFIRMED);
            }
            // Verification/proof
            if (referenceNumber != null && !referenceNumber.isBlank()) payment.setReferenceNumber(referenceNumber);
            if (bankName != null && !bankName.isBlank()) payment.setBankName(bankName);
            if (payerName != null && !payerName.isBlank()) payment.setPayerName(payerName);
            paymentRepository.save(payment);
            ordersRepository.save(currentOrder);
        }
        cartService.getCart().clear();
        // Compute ETA (1-5 days)
        int etaDays = new Random().nextInt(5) + 1;
        LocalDate etaDate = LocalDate.now().plusDays(etaDays);
        model.addAttribute("address", checkoutSession.getAddress());
        model.addAttribute("method", checkoutSession.getPaymentMethod());
        model.addAttribute("etaDays", etaDays);
        model.addAttribute("etaDate", etaDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        model.addAttribute("placed", true);
        // Also show orderRef for tracking
        model.addAttribute("orderRef", currentOrder != null ? currentOrder.getOrderRef() : null);
        // Notify admin on successful payment (all methods) with details
        if (currentOrder != null) {
            String userPart = currentOrder.getUser() != null ? currentOrder.getUser().getUsername() : "guest";
            String methodPart = method == CheckoutSession.PaymentMethod.CARD ? "CARD" : method == CheckoutSession.PaymentMethod.UPI_QR ? "UPI" : "COD";
            String proofPart = (referenceNumber != null && !referenceNumber.isBlank()) ? (" ref=" + referenceNumber) : "";
            String msg = "New order #" + currentOrder.getOrderId() + " by " + userPart + " | " + methodPart + proofPart + " | Total ₹" + String.format("%.2f", currentOrder.getTotal());
            notificationService.add("ORDER", msg, "/admin/orders");
        }
        return "checkout/confirmed";
    }

    @GetMapping("/confirmed")
    public String confirmed(Model model) {
        model.addAttribute("address", checkoutSession.getAddress());
        model.addAttribute("method", checkoutSession.getPaymentMethod());
        // Provide ETA on confirmation as well
        int etaDays = new Random().nextInt(5) + 1;
        LocalDate etaDate = LocalDate.now().plusDays(etaDays);
        model.addAttribute("etaDays", etaDays);
        model.addAttribute("etaDate", etaDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        if ((currentOrder == null || currentOrder.getOrderId() == null) && checkoutSession.getCurrentOrderId() != null) {
            currentOrder = ordersRepository.findById(checkoutSession.getCurrentOrderId()).orElse(null);
        }
        model.addAttribute("orderRef", currentOrder != null ? currentOrder.getOrderRef() : null);
        return "checkout/confirmed";
    }

    @PostMapping("/address/{id}/delete")
    public String deleteAddress(@PathVariable Integer id, Authentication auth, RedirectAttributes ra) {
        if (auth != null && auth.isAuthenticated()) {
            Optional<User> user = userRepository.findByUsername(auth.getName());
            Optional<Address> addr = addressRepository.findById(id);
            if (user.isPresent() && addr.isPresent() && addr.get().getUser() != null && addr.get().getUser().getUserId().equals(user.get().getUserId())) {
                long used = ordersRepository.countByAddress(addr.get());
                if (used > 0) {
                    ra.addAttribute("error", "Cannot delete: address linked to existing orders");
                    return "redirect:/checkout/address";
                }
                addressRepository.deleteById(id);
                ra.addAttribute("success", "Address deleted");
            }
        }
        return "redirect:/checkout/address";
    }

    @PostMapping("/address/{id}/edit")
    public String editAddress(@PathVariable Integer id,
                              @RequestParam String fullName,
                              @RequestParam String phone,
                              @RequestParam String addressLine1,
                              @RequestParam(required = false) String addressLine2,
                              @RequestParam(required = false) String landmark,
                              @RequestParam String area,
                              @RequestParam String city,
                              @RequestParam String state,
                              @RequestParam String postalCode,
                              @RequestParam String country,
                              @RequestParam(required = false) String label,
                              Authentication auth,
                              RedirectAttributes ra) {
        if (auth != null && auth.isAuthenticated()) {
            Optional<User> user = userRepository.findByUsername(auth.getName());
            Optional<Address> opt = addressRepository.findById(id);
            if (user.isPresent() && opt.isPresent() && opt.get().getUser() != null && opt.get().getUser().getUserId().equals(user.get().getUserId())) {
                Address a = opt.get();
                a.setFullName(fullName);
                a.setPhone(phone);
                a.setAddressLine1(addressLine1);
                a.setAddressLine2(addressLine2);
                a.setLandmark(landmark);
                a.setArea(area);
                a.setCity(city);
                a.setState(state);
                a.setPostalCode(postalCode);
                a.setCountry(country);
                a.setLabel((label != null && !label.isBlank()) ? label : a.getLabel());
                addressRepository.save(a);
                ra.addAttribute("success", "Address updated");
            }
        }
        return "redirect:/checkout/address";
    }
} 