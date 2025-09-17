package com.example.ecom.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class ImageProxyController {

    @GetMapping("/img-proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam(value = "url", required = false) String url,
                                        @RequestParam(value = "path", required = false) String localPath,
                                        @RequestParam(value = "w", required = false) Integer w,
                                        @RequestParam(value = "h", required = false) Integer h) {
        try {
            // Normalize inputs
            if (url != null) {
                url = url.trim();
                // Strip any leading non-url chars like '@' before http
                int httpIdx = url.indexOf("http");
                if (httpIdx > 0) {
                    url = url.substring(httpIdx);
                }
            }
            if (localPath != null) {
                localPath = localPath.trim();
            }

            byte[] sourceBytes;
            String contentType = "image/jpeg";

            if (url != null && url.startsWith("/")) {
                // Treat as local path under uploads
                localPath = url;
                url = null;
            }

            if (localPath != null) {
                // Serve from local uploads directory only
                if (!localPath.startsWith("/uploads/") && !localPath.startsWith("uploads/")) {
                    return ResponseEntity.badRequest().build();
                }
                Path base = Paths.get("uploads").toAbsolutePath().normalize();
                Path requested = Paths.get(localPath.replaceFirst("^/", "")).toAbsolutePath().normalize();
                if (!requested.startsWith(base)) {
                    return ResponseEntity.badRequest().build();
                }
                File f = requested.toFile();
                if (!f.exists() || !f.isFile()) return ResponseEntity.notFound().build();
                sourceBytes = Files.readAllBytes(requested);
                String probe = Files.probeContentType(requested);
                if (probe != null && probe.startsWith("image/")) contentType = probe;
            } else if (url != null) {
                // Remote fetch (http/https only)
                URI uri = new URI(url);
                if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                    return ResponseEntity.badRequest().build();
                }
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123 Safari/537.36");
                conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
                conn.setRequestProperty("Connection", "keep-alive");
                String origin = uri.getScheme() + "://" + uri.getHost() + "/";
                conn.setRequestProperty("Referer", origin);
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (InputStream is = conn.getInputStream()) {
                        sourceBytes = is.readAllBytes();
                        String ct = conn.getContentType();
                        if (ct != null && ct.startsWith("image/")) contentType = ct;
                    }
                } else if (code >= 300 && code < 400) {
                    String loc = conn.getHeaderField("Location");
                    if (loc != null) {
                        return ResponseEntity.status(302)
                                .header(HttpHeaders.LOCATION, loc)
                                .build();
                    } else {
                        return ResponseEntity.notFound().build();
                    }
                } else {
                    // Fallback: redirect browser to original URL
                    return ResponseEntity.status(302)
                            .header(HttpHeaders.LOCATION, url)
                            .build();
                }
            } else {
                return ResponseEntity.badRequest().build();
            }

            // Optional resizing
            byte[] outBytes = sourceBytes;
            if ((w != null && w > 0) || (h != null && h > 0)) {
                try {
                    BufferedImage input = ImageIO.read(new java.io.ByteArrayInputStream(sourceBytes));
                    if (input != null) {
                        int targetW = (w != null && w > 0) ? w : input.getWidth();
                        int targetH = (h != null && h > 0) ? h : input.getHeight();
                        // Preserve aspect ratio if only one provided
                        if (w != null && (h == null || h == 0)) {
                            targetH = Math.max(1, input.getHeight() * targetW / input.getWidth());
                        } else if (h != null && (w == null || w == 0)) {
                            targetW = Math.max(1, input.getWidth() * targetH / input.getHeight());
                        }
                        BufferedImage output = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = output.createGraphics();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.drawImage(input, 0, 0, targetW, targetH, null);
                        g2d.dispose();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        // Encode as JPEG for broad compatibility and smaller size
                        ImageIO.write(output, "jpg", baos);
                        outBytes = baos.toByteArray();
                        contentType = MediaType.IMAGE_JPEG_VALUE;
                    }
                } catch (Exception ignore) {
                    // fallback to original bytes
                    outBytes = sourceBytes;
                }
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(outBytes);
        } catch (Exception e) {
            // Final fallback: if we had a URL, redirect to it
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                return ResponseEntity.status(302).header(HttpHeaders.LOCATION, url).build();
            }
            return ResponseEntity.notFound().build();
        }
    }
} 