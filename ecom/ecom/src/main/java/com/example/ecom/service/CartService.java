package com.example.ecom.service;

import com.example.ecom.model.CartItem;
import com.example.ecom.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@SessionScope
public class CartService {
    private Map<Long, CartItem> cart = new LinkedHashMap<>();

    public void addProduct(Product product) {
        CartItem item = cart.getOrDefault(product.getProductId(), new CartItem(product, 0));
        item.setQuantity(item.getQuantity() + 1);
        cart.put(product.getProductId(), item);
    }
    
    public void updateProductQuantity(Long productId, int quantity) {
        if (cart.containsKey(productId)) {
            CartItem item = cart.get(productId);
            if (quantity > 0) {
                item.setQuantity(quantity);
            } else {
                cart.remove(productId);
            }
        }
    }

    public void removeProduct(Long productId) {
        cart.remove(productId);
    }

    public Map<Long, CartItem> getCart() {
        return cart;
    }

    public int getCartSize() {
        return cart.values().stream().mapToInt(CartItem::getQuantity).sum();
    }
}