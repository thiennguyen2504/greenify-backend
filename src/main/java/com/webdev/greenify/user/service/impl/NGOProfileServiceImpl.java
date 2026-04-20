package com.webdev.greenify.user.service.impl;

import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.common.exception.ResourceNotFoundException;
import com.webdev.greenify.file.entity.NGODocsImageEntity;
import com.webdev.greenify.file.entity.NGOProfileImageEntity;
import com.webdev.greenify.file.mapper.ImageMapper;
import com.webdev.greenify.station.entity.AddressEntity;
import com.webdev.greenify.station.mapper.AddressMapper;
import com.webdev.greenify.station.service.ProvinceNormalizationService;
import com.webdev.greenify.user.dto.NGOProfileFilterRequestDTO;
import com.webdev.greenify.user.dto.NGOProfileRejectRequestDTO;
import com.webdev.greenify.user.dto.NGOProfileRequestDTO;
import com.webdev.greenify.user.dto.NGOProfileResponseDTO;
import com.webdev.greenify.user.dto.PagedResponse;
import com.webdev.greenify.user.entity.NGOProfileEntity;
import com.webdev.greenify.user.entity.RoleEntity;
import com.webdev.greenify.user.entity.UserEntity;
import com.webdev.greenify.user.enumeration.ImageStatus;
import com.webdev.greenify.user.enumeration.NGOProfileStatus;
import com.webdev.greenify.user.mapper.NGOProfileMapper;
import com.webdev.greenify.user.repository.NGOProfileRepository;
import com.webdev.greenify.user.repository.RoleRepository;
import com.webdev.greenify.user.repository.UserRepository;
import com.webdev.greenify.user.service.NGOProfileService;
import com.webdev.greenify.user.specification.NGOProfileSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NGOProfileServiceImpl implements NGOProfileService {

    private final NGOProfileRepository ngoProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final NGOProfileMapper ngoProfileMapper;
    private final ImageMapper imageMapper;
    private final AddressMapper addressMapper;
    private final ProvinceNormalizationService provinceNormalizationService;

    @Override
    @Transactional
    public NGOProfileResponseDTO createNGOProfile(NGOProfileRequestDTO request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ngoProfileRepository.findByUserId(userId).ifPresent(p -> {
            throw new AppException("NGO profile already exists", HttpStatus.BAD_REQUEST);
        });

        NGOProfileEntity entity = ngoProfileMapper.toEntity(request);
        entity.setUser(user);
        entity.setStatus(NGOProfileStatus.PENDING_VERIFY);
        entity.setRejectedCount(0);

        if (request.getAddress() != null) {
            AddressEntity address = addressMapper.toAddressEntity(request.getAddress());
            normalizeAddressProvince(address);
            entity.setAddress(address);
        }

        if (request.getAvatar() != null) {
            NGOProfileImageEntity avatar = imageMapper.toNGOProfileImageEntity(request.getAvatar());
            avatar.setStatus(ImageStatus.ACTIVE);
            avatar.setNgoProfile(entity);
            entity.setAvatar(avatar);
        }

        if (request.getVerificationDocs() != null && !request.getVerificationDocs().isEmpty()) {
            List<NGODocsImageEntity> docs = request.getVerificationDocs().stream()
                    .map(docDto -> {
                        NGODocsImageEntity doc = imageMapper.toNGODocsImageEntity(docDto);
                        doc.setStatus(ImageStatus.ACTIVE);
                        doc.setNgoProfile(entity);
                        return doc;
                    }).toList();
            entity.setVerificationDocs(docs);
        }

        NGOProfileEntity saved = ngoProfileRepository.save(entity);
        return ngoProfileMapper.toDto(saved);
    }

    @Override
    @Transactional
    public NGOProfileResponseDTO updateNGOProfile(NGOProfileRequestDTO request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        NGOProfileEntity entity = ngoProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found"));

        if (entity.getStatus() == NGOProfileStatus.VERIFIED) {
            throw new AppException("Cannot update verified NGO profile", HttpStatus.BAD_REQUEST);
        }

        ngoProfileMapper.updateEntityFromDto(request, entity);
        entity.setStatus(NGOProfileStatus.PENDING_VERIFY);

        if (request.getAddress() != null) {
            if (entity.getAddress() != null) {
                addressMapper.updateAddress(entity.getAddress(), request.getAddress());
                normalizeAddressProvince(entity.getAddress());
            } else {
                AddressEntity address = addressMapper.toAddressEntity(request.getAddress());
                normalizeAddressProvince(address);
                entity.setAddress(address);
            }
        }

        if (request.getAvatar() != null) {
            if (entity.getAvatar() != null) {
                imageMapper.updateNGOProfileImage(request.getAvatar(), entity.getAvatar());
            } else {
                NGOProfileImageEntity avatar = imageMapper.toNGOProfileImageEntity(request.getAvatar());
                avatar.setStatus(ImageStatus.ACTIVE);
                avatar.setNgoProfile(entity);
                entity.setAvatar(avatar);
            }
        }

        if (request.getVerificationDocs() != null) {
            entity.getVerificationDocs().clear();
            List<NGODocsImageEntity> docs = request.getVerificationDocs().stream()
                    .map(docDto -> {
                        NGODocsImageEntity doc = imageMapper.toNGODocsImageEntity(docDto);
                        doc.setStatus(ImageStatus.ACTIVE);
                        doc.setNgoProfile(entity);
                        return doc;
                    }).toList();
            entity.getVerificationDocs().addAll(docs);
        }

        NGOProfileEntity saved = ngoProfileRepository.save(entity);
        return ngoProfileMapper.toDto(saved);
    }

    @Override
    @Transactional
    public NGOProfileResponseDTO approveNGOProfile(String id) {
        NGOProfileEntity entity = ngoProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found"));

        entity.setStatus(NGOProfileStatus.VERIFIED);
        entity.setRejectedReason(null);

        UserEntity user = entity.getUser();
        RoleEntity ngoRole = roleRepository.findByName("NGO")
                .orElseThrow(() -> new ResourceNotFoundException("NGO role not found"));
        user.getRoles().add(ngoRole);
        userRepository.save(user);

        return ngoProfileMapper.toDto(ngoProfileRepository.save(entity));
    }

    @Override
    @Transactional
    public NGOProfileResponseDTO rejectNGOProfile(String id, NGOProfileRejectRequestDTO request) {
        NGOProfileEntity entity = ngoProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found"));

        entity.setStatus(NGOProfileStatus.REJECTED);
        entity.setRejectedReason(request.getReason());
        entity.setRejectedCount((entity.getRejectedCount() == null ? 0 : entity.getRejectedCount()) + 1);

        return ngoProfileMapper.toDto(ngoProfileRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public NGOProfileResponseDTO getCurrentNGOProfile() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        NGOProfileEntity entity = ngoProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found"));
        return ngoProfileMapper.toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NGOProfileResponseDTO> searchNGOProfiles(NGOProfileFilterRequestDTO filter) {
        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Specification<NGOProfileEntity> spec = NGOProfileSpecification.buildSpecification(
                filter.getOrgName(),
                filter.getStatus(),
                filter.getSearch()
        );

        Page<NGOProfileEntity> page = ngoProfileRepository.findAll(spec, pageable);

        List<NGOProfileResponseDTO> content = page.getContent().stream()
                .map(ngoProfileMapper::toDto)
                .toList();

        return PagedResponse.of(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public NGOProfileResponseDTO getNGOProfileById(String id) {
        NGOProfileEntity entity = ngoProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found"));

        if (entity.getStatus() != NGOProfileStatus.VERIFIED) {
            throw new ResourceNotFoundException("NGO profile not found");
        }

        return ngoProfileMapper.toDto(entity);
    }

    private void normalizeAddressProvince(AddressEntity address) {
        if (address == null) {
            return;
        }
        address.setProvince(provinceNormalizationService.normalizeProvinceName(address.getProvince()));
    }
}
