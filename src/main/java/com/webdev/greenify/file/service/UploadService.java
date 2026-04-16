package com.webdev.greenify.file.service;

import com.webdev.greenify.file.dto.ImageResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UploadService {

    List<ImageResponseDTO> uploadImage(List<MultipartFile> files);
}
