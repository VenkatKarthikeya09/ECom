package com.example.ecom.controller;

import com.example.ecom.model.Category;
import com.example.ecom.model.Product;
import com.example.ecom.model.Role;
import com.example.ecom.model.User;
import com.example.ecom.model.Orders;
import com.example.ecom.model.Payment;
import com.example.ecom.model.Address;
import com.example.ecom.repository.AddressRepository;
import com.example.ecom.repository.OrdersRepository;
import com.example.ecom.repository.PaymentRepository;
import com.example.ecom.repository.UserRepository;
import com.example.ecom.service.CategoryService;
import com.example.ecom.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;
    @Autowired private UserRepository userRepository;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired private OrdersRepository ordersRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private AddressRepository addressRepository;

    @GetMapping
    public String adminIndex(Model model) {
        model.addAttribute("productCount", productService.getAllProducts().size());
        model.addAttribute("categoryCount", categoryService.getAllCategories().size());
        model.addAttribute("userCount", userRepository.count());
        List<User> latestUsers = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getUserId).reversed())
                .limit(5)
                .collect(Collectors.toList());
        List<Product> latestProducts = productService.getAllProducts().stream()
                .sorted(Comparator.comparing(Product::getProductId).reversed())
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("latestUsers", latestUsers);
        model.addAttribute("latestProducts", latestProducts);
        return "admin/index";
    }

    // PRODUCTS
    @GetMapping("/products")
    public String listProducts(Model model,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "25") int size,
                               @RequestParam(value = "success", required = false) String success,
                               @RequestParam(value = "error", required = false) String error) {
        Page<Product> productPage = productService.getProductsPage(page, size);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("page", productPage);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "admin/products";
    }

    @PostMapping("/products")
    public String createProduct(@ModelAttribute Product product, @RequestParam Integer categoryId, RedirectAttributes ra) {
        Category c = categoryService.getCategoryById(categoryId).orElse(null);
        product.setCategory(c);
        productService.save(product);
        ra.addAttribute("success", "Product created");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Long id, @ModelAttribute Product product, @RequestParam Integer categoryId, RedirectAttributes ra) {
        Category c = categoryService.getCategoryById(categoryId).orElse(null);
        product.setCategory(c);
        productService.update(id, product);
        ra.addAttribute("success", "Product updated");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes ra) {
        productService.deleteById(id);
        ra.addAttribute("success", "Product deleted");
        return "redirect:/admin/products";
    }

    // CATEGORIES
    @GetMapping("/categories")
    public String listCategories(Model model, @RequestParam(value = "success", required = false) String success,
                                 @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam String name, RedirectAttributes ra) {
        categoryService.createCategory(name);
        ra.addAttribute("success", "Category created");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(@PathVariable Integer id, @RequestParam String name, RedirectAttributes ra) {
        categoryService.updateCategory(id, name);
        ra.addAttribute("success", "Category updated");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes ra) {
        boolean ok = categoryService.deleteCategory(id);
        if (ok) {
            ra.addAttribute("success", "Category deleted");
        } else {
            ra.addAttribute("error", "Cannot delete: category has products");
        }
        return "redirect:/admin/categories";
    }

    // USERS
    @GetMapping("/users")
    public String listUsers(Model model, @RequestParam(value = "success", required = false) String success,
                            @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "admin/users";
    }

    @PostMapping("/users")
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String firstName,
                             @RequestParam String lastName,
                             @RequestParam String password,
                             @RequestParam String role,
                             RedirectAttributes ra) {
        if (userRepository.findByUsername(username).isPresent()) {
            ra.addAttribute("error", "Username already exists");
            return "redirect:/admin/users";
        }
        if (userRepository.findByEmail(email).isPresent()) {
            ra.addAttribute("error", "Email already exists");
            return "redirect:/admin/users";
        }
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setPassword(passwordEncoder.encode(password));
        u.setRole("ADMIN".equalsIgnoreCase(role) ? Role.ADMIN : Role.CUSTOMER);
        userRepository.save(u);
        ra.addAttribute("success", "User created");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Integer id,
                             @RequestParam String email,
                             @RequestParam String firstName,
                             @RequestParam String lastName,
                             @RequestParam String role,
                             RedirectAttributes ra) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isPresent()) {
            User u = opt.get();
            u.setEmail(email);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setRole("ADMIN".equalsIgnoreCase(role) ? Role.ADMIN : Role.CUSTOMER);
            userRepository.save(u);
            ra.addAttribute("success", "User updated");
        } else {
            ra.addAttribute("error", "User not found");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/password")
    public String resetPassword(@PathVariable Integer id, @RequestParam String password, RedirectAttributes ra) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isPresent()) {
            User u = opt.get();
            u.setPassword(passwordEncoder.encode(password));
            userRepository.save(u);
            ra.addAttribute("success", "Password reset");
        } else {
            ra.addAttribute("error", "User not found");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Integer id, RedirectAttributes ra) {
        userRepository.deleteById(id);
        ra.addAttribute("success", "User deleted");
        return "redirect:/admin/users";
    }

    // ORDERS
    @GetMapping("/orders")
    public String listOrders(Model model, @RequestParam(value = "success", required = false) String success,
                             @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("orders", ordersRepository.findAll());
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Integer id, @RequestParam Orders.Status status, RedirectAttributes ra) {
        Optional<Orders> opt = ordersRepository.findById(id);
        if (opt.isPresent()) {
            Orders o = opt.get();
            o.setStatus(status);
            ordersRepository.save(o);
            ra.addAttribute("success", "Order status updated");
        } else {
            ra.addAttribute("error", "Order not found");
        }
        return "redirect:/admin/orders";
    }

    // PAYMENTS
    @GetMapping("/payments")
    public String listPayments(Model model, @RequestParam(value = "success", required = false) String success,
                               @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("payments", paymentRepository.findAll());
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "admin/payments";
    }

    @PostMapping("/payments/{id}/status")
    public String updatePaymentStatus(@PathVariable Integer id, @RequestParam Payment.Status status, RedirectAttributes ra) {
        Optional<Payment> opt = paymentRepository.findById(id);
        if (opt.isPresent()) {
            Payment p = opt.get();
            p.setStatus(status);
            paymentRepository.save(p);
            ra.addAttribute("success", "Payment status updated");
        } else {
            ra.addAttribute("error", "Payment not found");
        }
        return "redirect:/admin/payments";
    }

    // ADDRESSES
    @GetMapping("/addresses")
    public String listAddresses(Model model, @RequestParam(value = "success", required = false) String success,
                                @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("addresses", addressRepository.findAll());
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "admin/addresses";
    }

    @PostMapping("/addresses/{id}/delete")
    public String adminDeleteAddress(@PathVariable Integer id, RedirectAttributes ra) {
        Optional<Address> opt = addressRepository.findById(id);
        if (opt.isPresent()) {
            Address a = opt.get();
            long used = ordersRepository.countByAddress(a);
            if (used > 0) {
                ra.addAttribute("error", "Cannot delete: address linked to existing orders");
            } else {
                addressRepository.delete(a);
                ra.addAttribute("success", "Address deleted");
            }
        } else {
            ra.addAttribute("error", "Address not found");
        }
        return "redirect:/admin/addresses";
    }
} 