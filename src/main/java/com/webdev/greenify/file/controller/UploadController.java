package com.webdev.greenify.file.controller;

import com.webdev.greenify.file.dto.ImageResponseDTO;
import com.webdev.greenify.file.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/upload")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    ResponseEntity<List<ImageResponseDTO>> upload(@RequestParam("files") List<MultipartFile> request) {
        return ResponseEntity.ok(uploadService.uploadImage(request));
    }
}
