package com.webdev.greenify.file.repository;

import com.webdev.greenify.file.entity.NGOProfileImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NGOProfileImageRepository extends JpaRepository<NGOProfileImageEntity, String> {
}
