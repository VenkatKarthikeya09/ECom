package com.example.ecom.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Controller
public class ImageProxyController {

    @GetMapping("/img-proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam("url") String url) {
        try {
            // Basic allowlist: only http/https
            URI uri = new URI(url);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                return ResponseEntity.badRequest().build();
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (InputStream is = conn.getInputStream()) {
                    byte[] bytes = is.readAllBytes();
                    String contentType = conn.getContentType();
                    MediaType mt = (contentType != null && contentType.startsWith("image/")) ? MediaType.parseMediaType(contentType) : MediaType.IMAGE_JPEG;
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                            .contentType(mt)
                            .body(bytes);
                }
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
} 