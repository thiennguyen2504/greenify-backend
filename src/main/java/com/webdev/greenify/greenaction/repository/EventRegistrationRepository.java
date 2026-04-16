package com.webdev.greenify.greenaction.repository;

import com.webdev.greenify.greenaction.entity.EventRegistrationEntity;
import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistrationEntity, String> {
    
    boolean existsByEventIdAndUserIdAndStatusNot(String eventId, String userId, RegistrationStatus status);

    long countByEventIdAndStatus(String eventId, RegistrationStatus status);

    @Query("SELECT r FROM EventRegistrationEntity r WHERE r.event.id = :eventId AND r.status = 'WAITLISTED' ORDER BY r.createdAt ASC")
    List<EventRegistrationEntity> findTopWaitlistedByEventId(String eventId);

    Optional<EventRegistrationEntity> findByIdAndUserId(String id, String userId);
}
