package com.webdev.greenify.file.repository;

import com.webdev.greenify.file.entity.PostImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostImageRepository extends JpaRepository<PostImageEntity, String> {
    Optional<PostImageEntity> findByPostId(String postId);
}
