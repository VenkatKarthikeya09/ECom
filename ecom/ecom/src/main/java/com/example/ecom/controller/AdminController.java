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
import com.example.ecom.repository.OrderItemRepository;
import com.example.ecom.service.AdminNotificationService;
import com.example.ecom.service.CategoryService;
import com.example.ecom.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    @Autowired private AdminNotificationService notificationService;
    @Autowired private OrderItemRepository orderItemRepository;

    @GetMapping
    public String adminIndex(Model model) {
        model.addAttribute("productCount", productService.getAllProducts().size());
        model.addAttribute("categoryCount", categoryService.getAllCategories().size());
        model.addAttribute("userCount", userRepository.count());
        List<User> latestUsers = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getUserId).reversed())
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
        List<Product> latestProducts = productService.getAllProducts().stream()
                .sorted(Comparator.comparing(Product::getProductId).reversed())
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("latestUsers", latestUsers);
        model.addAttribute("latestProducts", latestProducts);
        model.addAttribute("notifications", notificationService.all());
        java.util.List<com.example.ecom.service.AdminNotificationService.Notification> unread = notificationService.all().stream()
                .filter(n -> !n.read)
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("unreadNotifications", unread);
        return "admin/index";
    }

    @GetMapping("/notifications")
    @ResponseBody
    public List<AdminNotificationService.Notification> notificationsApi(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        return notificationService.latest(limit);
    }

    @PostMapping("/notifications/{id}/read")
    @ResponseBody
    public String markNotificationRead(@PathVariable("id") long id) {
        notificationService.markRead(id);
        return "OK";
    }

    @GetMapping("/notifications/all")
    public String notificationsPage(Model model) {
        model.addAttribute("notifications", notificationService.all());
        return "admin/notifications";
    }

    // PRODUCTS
    @GetMapping("/products")
    public String listProducts(Model model,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "25") int size,
                               @RequestParam(value = "q", required = false) String q,
                               @RequestParam(value = "categoryId", required = false) Integer categoryId,
                               @RequestParam(value = "minPrice", required = false) Double minPrice,
                               @RequestParam(value = "maxPrice", required = false) Double maxPrice,
                               @RequestParam(value = "minStock", required = false) Integer minStock,
                               @RequestParam(value = "maxStock", required = false) Integer maxStock,
                               @RequestParam(value = "success", required = false) String success,
                               @RequestParam(value = "error", required = false) String error) {
        // Start with all products and filter, then paginate the filtered result
        java.util.List<Product> all = productService.getAllProducts();
        java.util.List<Product> filtered = all;
        if (q != null && !q.isBlank()) {
            String query = q.trim().toLowerCase();
            // Numeric search: if q is a number, match price or stock as well
            Double qNum = null; Integer qInt = null;
            try { qNum = Double.parseDouble(query); } catch (Exception ignored) {}
            try { qInt = Integer.parseInt(query); } catch (Exception ignored) {}
            Double finalQNum = qNum; Integer finalQInt = qInt;
            filtered = filtered.stream().filter(p ->
                (p.getName() != null && p.getName().toLowerCase().contains(query)) ||
                (p.getDescription() != null && p.getDescription().toLowerCase().contains(query)) ||
                (finalQNum != null && Double.compare(p.getPrice(), finalQNum) == 0) ||
                (finalQInt != null && p.getStockQuantity() == finalQInt)
            ).collect(java.util.stream.Collectors.toList());
        }
        if (categoryId != null) {
            filtered = filtered.stream().filter(p -> p.getCategory() != null && p.getCategory().getCategoryId().equals(categoryId)).collect(java.util.stream.Collectors.toList());
        }
        if (minPrice != null) {
            filtered = filtered.stream().filter(p -> p.getPrice() >= minPrice).collect(java.util.stream.Collectors.toList());
        }
        if (maxPrice != null) {
            filtered = filtered.stream().filter(p -> p.getPrice() <= maxPrice).collect(java.util.stream.Collectors.toList());
        }
        if (minStock != null) {
            filtered = filtered.stream().filter(p -> p.getStockQuantity() >= minStock).collect(java.util.stream.Collectors.toList());
        }
        if (maxStock != null) {
            filtered = filtered.stream().filter(p -> p.getStockQuantity() <= maxStock).collect(java.util.stream.Collectors.toList());
        }

        int from = Math.max(0, Math.min(page * size, filtered.size()));
        int to = Math.max(from, Math.min(from + size, filtered.size()));
        java.util.List<Product> pageContent = filtered.subList(from, to);
        Page<Product> pageObj = new PageImpl<>(pageContent, PageRequest.of(page, size), filtered.size());

        model.addAttribute("products", pageContent);
        model.addAttribute("page", pageObj);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minStock", minStock);
        model.addAttribute("maxStock", maxStock);
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
                                 @RequestParam(value = "error", required = false) String error,
                                 @RequestParam(value = "sort", required = false, defaultValue = "nameAsc") String sort) {
        List<Category> cats = categoryService.getAllCategories();
        switch (sort) {
            case "nameDesc":
                cats = cats.stream().sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER).reversed()).collect(Collectors.toList());
                break;
            case "oldest":
                cats = cats.stream().sorted(Comparator.comparing(Category::getCategoryId)).collect(Collectors.toList());
                break;
            case "newest":
                cats = cats.stream().sorted(Comparator.comparing(Category::getCategoryId).reversed()).collect(Collectors.toList());
                break;
            default:
                cats = cats.stream().sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList());
        }
        model.addAttribute("categories", cats);
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("sort", sort);
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
                            @RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort) {
        List<User> users = userRepository.findAll();
        switch (sort) {
            case "oldest": users = users.stream().sorted(Comparator.comparing(User::getUserId)).collect(Collectors.toList()); break;
            case "username": users = users.stream().sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList()); break;
            case "email": users = users.stream().sorted(Comparator.comparing(User::getEmail, String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList()); break;
            case "role": users = users.stream().sorted(Comparator.comparing(u -> u.getRole().name())).collect(Collectors.toList()); break;
            default: users = users.stream().sorted(Comparator.comparing(User::getUserId).reversed()).collect(Collectors.toList());
        }
        model.addAttribute("users", users);
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("sort", sort);
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
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addAttribute("error", "User not found");
            return "redirect:/admin/users";
        }
        User u = opt.get();
        long addrCount = addressRepository.countByUser(u);
        long orderCount = ordersRepository.findByUser(u).size();
        if (addrCount > 0 || orderCount > 0) {
            String reason = addrCount > 0 ? "addresses" : "orders";
            ra.addAttribute("error", "Cannot delete: user linked with existing " + reason);
            return "redirect:/admin/users";
        }
        userRepository.deleteById(id);
        ra.addAttribute("success", "User deleted");
        return "redirect:/admin/users";
    }

    // ORDERS
    @GetMapping("/orders")
    public String listOrders(Model model, @RequestParam(value = "success", required = false) String success,
                             @RequestParam(value = "error", required = false) String error,
                             @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort) {
        List<Orders> all = ordersRepository.findAll();
        if ("oldest".equalsIgnoreCase(sort)) {
            all = all.stream().sorted(Comparator.comparing(Orders::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        } else {
            all = all.stream().sorted(Comparator.comparing(Orders::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).collect(Collectors.toList());
        }
        model.addAttribute("orders", all);
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("sort", sort);
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
                               @RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort) {
        List<Payment> list = paymentRepository.findAll();
        switch (sort) {
            case "oldest": list = list.stream().sorted(Comparator.comparing(Payment::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList()); break;
            case "amountAsc": list = list.stream().sorted(Comparator.comparing(Payment::getAmount)).collect(Collectors.toList()); break;
            case "amountDesc": list = list.stream().sorted(Comparator.comparing(Payment::getAmount).reversed()).collect(Collectors.toList()); break;
            case "status": list = list.stream().sorted(Comparator.comparing(p -> p.getStatus().name())).collect(Collectors.toList()); break;
            default: list = list.stream().sorted(Comparator.comparing(Payment::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).collect(Collectors.toList());
        }
        model.addAttribute("payments", list);
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("sort", sort);
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
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort) {
        List<Address> addrs = addressRepository.findAll();
        switch (sort) {
            case "oldest": addrs = addrs.stream().sorted(Comparator.comparing(Address::getAddressId)).collect(Collectors.toList()); break;
            case "city": addrs = addrs.stream().sorted(Comparator.comparing(Address::getCity, String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList()); break;
            case "state": addrs = addrs.stream().sorted(Comparator.comparing(Address::getState, String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList()); break;
            case "user": addrs = addrs.stream().sorted(Comparator.comparing(a -> a.getUser() != null ? a.getUser().getUsername() : "", String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList()); break;
            default: addrs = addrs.stream().sorted(Comparator.comparing(Address::getAddressId).reversed()).collect(Collectors.toList());
        }
        model.addAttribute("addresses", addrs);
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("sort", sort);
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