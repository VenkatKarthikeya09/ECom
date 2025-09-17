package com.example.ecom.controller;

import com.example.ecom.model.CartItem;
import com.example.ecom.model.Product;
import com.example.ecom.service.CartService;
import com.example.ecom.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @GetMapping("/cart")
    public String viewCart(Model model) {
        model.addAttribute("cartItems", cartService.getCart().values());
        model.addAttribute("totalPrice", calculateTotalPrice());
        return "cart";
    }

    @PostMapping("/update-quantity/{productId}")
    @ResponseBody
    public Map<String, Object> updateQuantity(@PathVariable Long productId, @RequestParam int quantity) {
        cartService.updateProductQuantity(productId, quantity);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("cartSize", cartService.getCartSize());
        response.put("totalPrice", calculateTotalPrice());
        Optional<CartItem> updatedItem = cartService.getCart().values().stream()
                .filter(item -> item.getProduct().getProductId().equals(productId))
                .findFirst();
        updatedItem.ifPresent(item -> response.put("itemTotal", item.getProduct().getPrice() * item.getQuantity()));
        return response;
    }

    @PostMapping("/remove-from-cart/{productId}")
    @ResponseBody
    public Map<String, Object> removeFromCart(@PathVariable Long productId) {
        cartService.removeProduct(productId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("cartSize", cartService.getCartSize());
        response.put("totalPrice", calculateTotalPrice());
        return response;
    }

    @PostMapping("/api/cart/merge")
    @ResponseBody
    public Map<String, Object> mergeGuestCart(@RequestBody List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            Long productId = Long.valueOf(String.valueOf(item.get("productId")));
            int quantity = Integer.parseInt(String.valueOf(item.getOrDefault("quantity", 1)));
            Optional<Product> p = productService.getProductById(productId);
            if (p.isPresent()) {
                for (int i = 0; i < quantity; i++) {
                    cartService.addProduct(p.get());
                }
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("cartSize", cartService.getCartSize());
        response.put("totalPrice", calculateTotalPrice());
        return response;
    }

    @GetMapping("/api/cart/size")
    @ResponseBody
    public Map<String, Object> cartSize() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("cartSize", cartService.getCartSize());
        return response;
    }

    private double calculateTotalPrice() {
        return cartService.getCart().values().stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
    }
}