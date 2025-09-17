package com.example.ecom.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AdminNotificationService {
    public static class Notification {
        public long id;
        public String type; // ORDER, CANCEL, STOCK
        public String message;
        public String link; // optional link to details
        public boolean read = false;
        public Instant createdAt = Instant.now();
        public Notification(String type, String message) { this.type = type; this.message = message; }
        public Notification(String type, String message, String link) { this.type = type; this.message = message; this.link = link; }
    }

    private final Deque<Notification> queue = new ArrayDeque<>();
    private final AtomicLong idSeq = new AtomicLong(1);
    private final int capacity = 1000;

    public synchronized void add(String type, String message) {
        if (queue.size() >= capacity) queue.removeFirst();
        Notification n = new Notification(type, message);
        n.id = idSeq.getAndIncrement();
        n.read = false;
        queue.addLast(n);
    }

    public synchronized void add(String type, String message, String link) {
        if (queue.size() >= capacity) queue.removeFirst();
        Notification n = new Notification(type, message, link);
        n.id = idSeq.getAndIncrement();
        n.read = false;
        queue.addLast(n);
    }

    public synchronized List<Notification> latest(int limit) {
        List<Notification> all = new ArrayList<>(queue);
        int from = Math.max(0, all.size() - limit);
        return all.subList(from, all.size());
    }

    public synchronized List<Notification> all() {
        return new ArrayList<>(queue);
    }

    public synchronized void markRead(long id) {
        for (Notification n : queue) {
            if (n.id == id) { n.read = true; break; }
        }
    }
} 