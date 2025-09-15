package com.example.ecom.service;

import com.example.ecom.model.Category;
import com.example.ecom.model.Product;
import com.example.ecom.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private CategoryService categoryService;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Page<Product> getProductsPage(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size));
    }

    public List<Product> getProductsByCategoryId(Integer categoryId) {
        return productRepository.findByCategory_CategoryId(categoryId);
    }

    public List<Product> getProductsByIds(List<Long> productIds) {
        return productRepository.findAllById(productIds);
    }

    public List<Product> searchProducts(String query) {
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
    }

    /**
     * This method fetches a limited, random set of products from each category
     * to display on the home page. This avoids long pages and is more performant.
     */
    public Map<Category, List<Product>> getHomePageProducts() {
        List<Category> allCategories = categoryService.getAllCategories();
        Map<Category, List<Product>> categorizedProducts = new LinkedHashMap<>();

        for (Category category : allCategories) {
            // Fetch all products for the category
            List<Product> productsInCategory = productRepository.findByCategory_CategoryId(category.getCategoryId());

            // Shuffle and take a limited number (e.g., 4)
            Collections.shuffle(productsInCategory);
            List<Product> limitedProducts = productsInCategory.stream()
                    .limit(4)
                    .collect(Collectors.toList());

            // Add to the map only if there are products
            if (!limitedProducts.isEmpty()) {
                categorizedProducts.put(category, limitedProducts);
            }
        }
        return categorizedProducts;
    }

    public Optional<Product> getProductById(Long productId) {
        return productRepository.findById(productId);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public Optional<Product> update(Long id, Product updated) {
        return productRepository.findById(id).map(p -> {
            p.setName(updated.getName());
            p.setDescription(updated.getDescription());
            p.setPrice(updated.getPrice());
            p.setImageUrl(updated.getImageUrl());
            p.setStockQuantity(updated.getStockQuantity());
            p.setCategory(updated.getCategory());
            return productRepository.save(p);
        });
    }

    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }
}