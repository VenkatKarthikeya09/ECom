package com.example.ecom.controller;

import com.example.ecom.model.Orders;
import com.example.ecom.model.Payment;
import com.example.ecom.model.User;
import com.example.ecom.model.OrderItem;
import com.example.ecom.repository.OrdersRepository;
import com.example.ecom.repository.PaymentRepository;
import com.example.ecom.repository.UserRepository;
import com.example.ecom.repository.OrderItemRepository;
import com.example.ecom.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
	@Autowired private OrderItemRepository orderItemRepository;
	@Autowired private ProductService productService;

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
		Map<Integer, Boolean> canCancelMap = new HashMap<>();
		Map<Integer, Boolean> canRemoveMap = new HashMap<>();
		for (Orders o : orders) {
			canCancelMap.put(o.getOrderId(), canCancelOrder(o));
			canRemoveMap.put(o.getOrderId(), o.getStatus() == Orders.Status.CREATED);
		}
		model.addAttribute("orders", orders);
		model.addAttribute("canCancelMap", canCancelMap);
		model.addAttribute("canRemoveMap", canRemoveMap);
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
		model.addAttribute("canCancel", canCancelOrder(order));
		model.addAttribute("canRemove", order.getStatus() == Orders.Status.CREATED);
		return "orders/detail";
	}

	// Allow customer to cancel own order (for paid/confirmed before out-for-delivery)
	@PostMapping("/{id}/cancel")
	public String cancel(@PathVariable Integer id, Authentication auth, RedirectAttributes ra) {
		if (auth == null || !auth.isAuthenticated()) return "redirect:/login";
		Optional<User> user = userRepository.findByUsername(auth.getName());
		Optional<Orders> opt = ordersRepository.findById(id);
		if (user.isPresent() && opt.isPresent()) {
			Orders o = opt.get();
			boolean ownsByUser = o.getUser() != null && o.getUser().getUserId().equals(user.get().getUserId());
			boolean ownsByEmail = o.getAddress() != null && o.getAddress().getEmail() != null && o.getAddress().getEmail().equalsIgnoreCase(user.get().getEmail());
			if (ownsByUser || ownsByEmail) {
				if (!canCancelOrder(o)) {
					ra.addAttribute("error", "Cannot cancel at this stage (out for delivery).");
					return "redirect:/orders";
				}
				o.setStatus(Orders.Status.CANCELLED);
				ordersRepository.save(o);
				paymentRepository.findByOrder(o).ifPresent(p -> {
					if (p.getStatus() == Payment.Status.PAID || p.getStatus() == Payment.Status.SUBMITTED) {
						p.setStatus(Payment.Status.FAILED);
						paymentRepository.save(p);
					}
				});
				ra.addAttribute("success", "Order cancelled. Refund (if applicable) will be initiated in 3-5 days.");
			}
		}
		return "redirect:/orders";
	}

	// Remove un-paid order entirely and restock
	@PostMapping("/{id}/remove")
	public String removeUnpaid(@PathVariable Integer id, Authentication auth, RedirectAttributes ra) {
		if (auth == null || !auth.isAuthenticated()) return "redirect:/login";
		Optional<User> user = userRepository.findByUsername(auth.getName());
		Optional<Orders> opt = ordersRepository.findById(id);
		if (user.isPresent() && opt.isPresent()) {
			Orders o = opt.get();
			boolean ownsByUser = o.getUser() != null && o.getUser().getUserId().equals(user.get().getUserId());
			boolean ownsByEmail = o.getAddress() != null && o.getAddress().getEmail() != null && o.getAddress().getEmail().equalsIgnoreCase(user.get().getEmail());
			if ((ownsByUser || ownsByEmail) && o.getStatus() == Orders.Status.CREATED) {
				List<OrderItem> items = orderItemRepository.findByOrder(o);
				for (OrderItem it : items) {
					if (it.getProduct() != null) {
						it.getProduct().setStockQuantity(it.getProduct().getStockQuantity() + it.getQuantity());
						productService.save(it.getProduct());
					}
				}
				// Delete order and its items
				items.forEach(orderItemRepository::delete);
				ordersRepository.delete(o);
				ra.addAttribute("success", "Removed from list.");
			}
		}
		return "redirect:/orders";
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
		model.addAttribute("canCancel", canCancelOrder(found));
		model.addAttribute("canRemove", found.getStatus() == Orders.Status.CREATED);
		return "orders/detail";
	}

	private boolean canCancelOrder(Orders order) {
		if (order.getStatus() != Orders.Status.PAID && order.getStatus() != Orders.Status.COD_CONFIRMED) return false;
		// Determine current step; disallow if 'Out for delivery' or later (index >= 3)
		Instant created = order.getCreatedAt() != null ? order.getCreatedAt() : Instant.now();
		Instant eta = created.plusSeconds(4L * 24 * 3600);
		long total = Math.max(1, eta.getEpochSecond() - created.getEpochSecond());
		long step = total / 4; // 4 segments between 5 labels
		Instant now = Instant.now();
		int currentIdx = (int)Math.min(4, Math.max(0, (now.getEpochSecond() - created.getEpochSecond()) / Math.max(1, step)));
		return currentIdx < 3; // before 'Out for delivery'
	}

	private void addProgressiveTimeline(Model model, Orders order) {
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		ZoneId zone = ZoneId.systemDefault();

		// If order is cancelled, show refund-centric timeline instead of delivery
		if (order.getStatus() == Orders.Status.CANCELLED) {
			Instant created = order.getCreatedAt() != null ? order.getCreatedAt() : Instant.now();
			Instant cancelled = order.getUpdatedAt() != null ? order.getUpdatedAt() : created;
			Instant refundCompleteEta = cancelled.plusSeconds(4L * 24 * 3600); // ~3-5 days window demo

			List<Map<String, String>> timeline = new ArrayList<>();

			Map<String, String> t1 = new HashMap<>();
			t1.put("label", "Order received");
			t1.put("status", "COMPLETED");
			t1.put("actual", fmt.format(LocalDateTime.ofInstant(created, zone)));
			t1.put("expected", fmt.format(LocalDateTime.ofInstant(created, zone)));
			timeline.add(t1);

			Map<String, String> t2 = new HashMap<>();
			t2.put("label", "Order cancelled");
			t2.put("status", "COMPLETED");
			t2.put("actual", fmt.format(LocalDateTime.ofInstant(cancelled, zone)));
			t2.put("expected", fmt.format(LocalDateTime.ofInstant(cancelled, zone)));
			timeline.add(t2);

			Map<String, String> t3 = new HashMap<>();
			t3.put("label", "Refund in process");
			boolean refundDone = Instant.now().isAfter(refundCompleteEta);
			t3.put("status", refundDone ? "COMPLETED" : "CURRENT");
			t3.put("expected", fmt.format(LocalDateTime.ofInstant(cancelled.plusSeconds(2L * 24 * 3600), zone)));
			if (refundDone) {
				t3.put("actual", fmt.format(LocalDateTime.ofInstant(refundCompleteEta.minusSeconds(1L * 24 * 3600), zone)));
			}
			timeline.add(t3);

			Map<String, String> t4 = new HashMap<>();
			t4.put("label", "Refund completed to source");
			t4.put("status", Instant.now().isAfter(refundCompleteEta) ? "COMPLETED" : "UPCOMING");
			t4.put("expected", fmt.format(LocalDateTime.ofInstant(refundCompleteEta, zone)));
			if (Instant.now().isAfter(refundCompleteEta)) {
				t4.put("actual", fmt.format(LocalDateTime.ofInstant(refundCompleteEta, zone)));
			}
			timeline.add(t4);

			model.addAttribute("timeline", timeline);
			model.addAttribute("etaDate", fmt.format(LocalDateTime.ofInstant(refundCompleteEta, zone)));
			return;
		}

		// Normal delivery timeline
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