package com.example.ecom.controller;

import com.example.ecom.model.Orders;
import com.example.ecom.model.Payment;
import com.example.ecom.model.User;
import com.example.ecom.repository.OrdersRepository;
import com.example.ecom.repository.PaymentRepository;
import com.example.ecom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Controller
@RequestMapping("/orders")
public class OrdersController {

	@Autowired private OrdersRepository ordersRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private PaymentRepository paymentRepository;

	@GetMapping
	public String list(Model model, Authentication auth) {
		if (auth == null || !auth.isAuthenticated()) {
			return "redirect:/login";
		}
		List<Orders> orders = new ArrayList<>();
		Optional<User> user = userRepository.findByUsername(auth.getName());
		if (user.isPresent()) {
			orders = ordersRepository.findByUser(user.get());
		}
		// Fallback: find by email on addresses if user linkage was missing
		if (orders.isEmpty() && user.isPresent()) {
			orders.addAll(ordersRepository.findByAddressEmail(user.get().getEmail()));
		}
		orders.sort(Comparator.comparing(Orders::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
		model.addAttribute("orders", orders);
		return "orders/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Integer id, Model model, Authentication auth) {
		if (auth == null || !auth.isAuthenticated()) {
			return "redirect:/login";
		}
		Optional<Orders> opt = ordersRepository.findById(id);
		if (opt.isEmpty()) return "redirect:/orders";
		Orders order = opt.get();
		model.addAttribute("order", order);
		model.addAttribute("items", order.getItems());
		// Payment details
		Payment payment = paymentRepository.findByOrder(order).orElse(null);
		model.addAttribute("payment", payment);
		// Progressive timeline and ETA
		addProgressiveTimeline(model, order);
		return "orders/detail";
	}

	// New: track by order reference (computed transient)
	@GetMapping("/track/{orderRef}")
	public String track(@PathVariable String orderRef, Model model, Authentication auth) {
		if (auth == null || !auth.isAuthenticated()) {
			return "redirect:/login";
		}
		List<Orders> all = ordersRepository.findAll();
		Orders found = all.stream().filter(o -> orderRef.equalsIgnoreCase(o.getOrderRef())).findFirst().orElse(null);
		if (found == null) {
			model.addAttribute("orders", new ArrayList<Orders>());
			return "orders/list";
		}
		model.addAttribute("order", found);
		model.addAttribute("items", found.getItems());
		Payment payment = paymentRepository.findByOrder(found).orElse(null);
		model.addAttribute("payment", payment);
		addProgressiveTimeline(model, found);
		return "orders/detail";
	}

	private void addProgressiveTimeline(Model model, Orders order) {
		String[] labels = new String[]{
			"Order received",
			"Sent to nearest facility",
			"Assigned delivery partner",
			"Out for delivery",
			"Delivered"
		};
		Instant created = order.getCreatedAt() != null ? order.getCreatedAt() : Instant.now();
		// ETA at created + 4 days for demo
		Instant eta = created.plusSeconds(4L * 24 * 3600);
		long total = Math.max(1, eta.getEpochSecond() - created.getEpochSecond());
		long step = total / (labels.length - 1); // 4 segments
		Instant now = Instant.now();
		int currentIdx = (int)Math.min(labels.length - 1, Math.max(0, (now.getEpochSecond() - created.getEpochSecond()) / Math.max(1, step)));
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		ZoneId zone = ZoneId.systemDefault();
		List<Map<String, String>> timeline = new ArrayList<>();
		for (int i = 0; i < labels.length; i++) {
			Instant expected = created.plusSeconds(step * i);
			Map<String, String> m = new HashMap<>();
			m.put("label", labels[i]);
			m.put("expected", fmt.format(LocalDateTime.ofInstant(expected, zone)));
			if (i < currentIdx) {
				m.put("status", "COMPLETED");
				m.put("actual", fmt.format(LocalDateTime.ofInstant(expected, zone)));
			} else if (i == currentIdx) {
				m.put("status", "CURRENT");
			} else {
				m.put("status", "UPCOMING");
			}
			timeline.add(m);
		}
		model.addAttribute("timeline", timeline);
		model.addAttribute("etaDate", fmt.format(LocalDateTime.ofInstant(eta, zone)));
	}
} 