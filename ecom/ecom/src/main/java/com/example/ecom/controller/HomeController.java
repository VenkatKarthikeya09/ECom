package com.example.ecom.controller;

import com.example.ecom.model.Category;
import com.example.ecom.model.Product;
import com.example.ecom.service.ProductService;
import com.example.ecom.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/")
    public String viewHomePage(Model model) {
        Map<Category, List<Product>> categorizedProducts = productService.getHomePageProducts();
        model.addAttribute("categorizedProducts", categorizedProducts);
        model.addAttribute("trending", getTrendingProducts());
        model.addAttribute("cartSize", 0);
        return "Home";
    }

    @GetMapping("/admin_home")
    public String adminHome() {
        return "admin_home";
    }

    private List<Product> getTrendingProducts() {
        List<Product> all = productService.getAllProducts();
        Collections.shuffle(all);
        return all.stream().limit(8).toList();
    }

    /**
     * API endpoint to get a list of recently searched products based on provided IDs.
     * This is called by JavaScript to populate the "Recently Searched" section.
     */
    @GetMapping("/api/products/recently-searched")
    @ResponseBody
    public List<Product> getRecentlySearched(@RequestParam List<Long> productIds) {
        // This method fetches the full product details for the given IDs.
        // It's a clean way to handle the frontend-stored history.
        return productService.getProductsByIds(productIds);
    }
    
    /**
     * API endpoint to get a list of suggested products.
     * This is a simple implementation that returns a random list for now.
     */
    @GetMapping("/api/products/suggested")
    @ResponseBody
    public List<Product> getSuggestedProducts(@RequestParam(value = "limit", required = false, defaultValue = "10") int limit) {
        // Here you would implement your recommendation logic (e.g., based on trending items, purchase history).
        // For now, we'll return a random sample of products.
        List<Product> allProducts = productService.getAllProducts();
        Collections.shuffle(allProducts);
        int safeLimit = Math.max(1, Math.min(limit, allProducts.size()));
        return allProducts.stream().limit(safeLimit).collect(Collectors.toList());
    }

    // Search API used previously by home live search (still available)
    @GetMapping("/api/products/search")
    @ResponseBody
    public List<Product> apiSearch(@RequestParam("query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return productService.searchProducts(query.trim());
    }

    // Search page to display results and similar items section
    @GetMapping("/search")
    public String searchPage(@RequestParam(value = "query", required = false) String query, Model model) {
        List<Product> results = (query == null || query.trim().isEmpty())
                ? Collections.emptyList()
                : productService.searchProducts(query.trim());
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("results", results);
        return "search";
    }

    // Product details page with similar items (same category)
    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable("id") Long id, Model model) {
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isEmpty()) {
            return "redirect:/";
        }
        Product product = productOpt.get();
        model.addAttribute("product", product);

        List<Product> similar = new ArrayList<>();
        if (product.getCategory() != null) {
            List<Product> inCategory = productService.getProductsByCategoryId(product.getCategory().getCategoryId());
            similar = inCategory.stream()
                    .filter(p -> !Objects.equals(p.getProductId(), product.getProductId()))
                    .limit(8)
                    .collect(Collectors.toList());
        }
        model.addAttribute("similarProducts", similar);
        return "product";
    }
        
}

@ControllerAdvice
class GlobalModelAttributes {
    @Autowired
    private CategoryService categoryService;

    @ModelAttribute("allCategories")
    public List<Category> populateCategories() {
        return categoryService.getAllCategories();
    }
}
