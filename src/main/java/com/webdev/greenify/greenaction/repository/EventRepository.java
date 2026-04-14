package com.webdev.greenify.greenaction.repository;

import com.webdev.greenify.greenaction.entity.EventEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, String>, JpaSpecificationExecutor<EventEntity> {

    @EntityGraph(attributePaths = {"images"})
    List<EventEntity> findByIdIn(Collection<String> ids);
}
