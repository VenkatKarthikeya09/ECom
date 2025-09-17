package com.example.ecom.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.ecom.model.Category;
import com.example.ecom.model.Product;
import com.example.ecom.service.CategoryService;
import com.example.ecom.service.ProductService;

@Controller
public class CategoryController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/category/{categoryId}")
    public String showCategory(@PathVariable Integer categoryId, Model model) {
        Optional<Category> category = categoryService.getCategoryById(categoryId);

        if (category.isPresent()) {
            List<Product> products = productService.getProductsByCategoryId(categoryId);
            products.forEach(p -> {
                if (p.getImageUrl() != null && !p.getImageUrl().isBlank() && !p.getImageUrl().startsWith("/img-proxy")) {
                    p.setImageUrl("/img-proxy?url=" + p.getImageUrl());
                }
            });
            model.addAttribute("categoryName", category.get().getName());
            model.addAttribute("products", products);
            return "category";
        } else {
            return "redirect:/"; // Redirect to home if category is not found
        }
    }
}