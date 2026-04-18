package com.webdev.greenify.file.repository;

import com.webdev.greenify.file.entity.NGODocsImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NGODocsImageRepository extends JpaRepository<NGODocsImageEntity, String> {
}
