package com.webdev.greenify.greenaction.repository;

import com.webdev.greenify.greenaction.entity.EventRegistrationEntity;
import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistrationEntity, String>, JpaSpecificationExecutor<EventRegistrationEntity> {
    
    boolean existsByEventIdAndUserIdAndStatusNot(String eventId, String userId, RegistrationStatus status);

    @Query("SELECT r FROM EventRegistrationEntity r WHERE r.event.id = :eventId")
    List<EventRegistrationEntity> findAllByEventId(String eventId);
    
    Optional<EventRegistrationEntity> findByEventIdAndUserId(String eventId, String userId);

    long countByEventIdAndStatus(String eventId, RegistrationStatus status);

    @Query("SELECT r FROM EventRegistrationEntity r WHERE r.event.id = :eventId AND r.status = 'WAITLISTED' ORDER BY r.createdAt ASC")
    List<EventRegistrationEntity> findTopWaitlistedByEventId(String eventId);

    Optional<EventRegistrationEntity> findByIdAndUserId(String id, String userId);
    
    Optional<EventRegistrationEntity> findByRegistrationCode(String registrationCode);
    
    @Query("""
            SELECT COUNT(r)
            FROM EventRegistrationEntity r
            WHERE r.status = :status
            AND r.checkInTime BETWEEN :start AND :end
            AND r.isDeleted = false
            """)
    long countByCheckInTimeBetweenAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") RegistrationStatus status);

    @Query("""
            SELECT COUNT(r)
            FROM EventRegistrationEntity r
            WHERE r.event.organizer.id = :organizerId
            AND r.createdAt BETWEEN :start AND :end
            AND r.isDeleted = false
            """)
    long countByOrganizerIdAndCreatedAtBetween(
            @Param("organizerId") String organizerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT COUNT(r)
            FROM EventRegistrationEntity r
            WHERE r.event.organizer.id = :organizerId
            AND r.status = :status
            AND r.createdAt BETWEEN :start AND :end
            AND r.isDeleted = false
            """)
    long countByOrganizerIdAndStatusAndCreatedAtBetween(
            @Param("organizerId") String organizerId,
            @Param("status") RegistrationStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
