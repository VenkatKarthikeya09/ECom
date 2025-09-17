package com.example.ecom.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/upload")
public class AdminUploadController {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null) ext = "jpg";
        String name = UUID.randomUUID().toString().replaceAll("-", "") + "-" + Instant.now().toEpochMilli() + "." + ext.toLowerCase();
        Path uploadDir = Paths.get("uploads");
        Files.createDirectories(uploadDir);
        Path target = uploadDir.resolve(name);
        Files.copy(file.getInputStream(), target);
        String url = "/uploads/" + name;
        return ResponseEntity.ok(Map.of("url", url));
    }
} 