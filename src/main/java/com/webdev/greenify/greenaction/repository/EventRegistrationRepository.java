package com.webdev.greenify.greenaction.repository;

import com.webdev.greenify.greenaction.entity.EventRegistrationEntity;
import com.webdev.greenify.greenaction.enumeration.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistrationEntity, String>, JpaSpecificationExecutor<EventRegistrationEntity> {

    boolean existsByEventIdAndUserIdAndRegistrationStatusNot(String eventId, String userId, RegistrationStatus registrationStatus);

    @Query("SELECT r FROM EventRegistrationEntity r WHERE r.event.id = :eventId AND r.isDeleted = false")
    List<EventRegistrationEntity> findAllByEventId(String eventId);

    Optional<EventRegistrationEntity> findByEventIdAndUserIdAndIsDeletedFalse(String eventId, String userId);

    @Query("SELECT r FROM EventRegistrationEntity r " +
        "WHERE r.user.id = :userId AND r.event.id IN :eventIds AND r.isDeleted = false")
    List<EventRegistrationEntity> findByUserIdAndEventIdIn(
        @Param("userId") String userId,
        @Param("eventIds") Collection<String> eventIds);

    long countByEventIdAndRegistrationStatus(String eventId, RegistrationStatus registrationStatus);

    @Query("SELECT r FROM EventRegistrationEntity r " +
        "WHERE r.event.id = :eventId AND r.registrationStatus = 'WAITLISTED' AND r.isDeleted = false " +
        "ORDER BY r.createdAt ASC")
    List<EventRegistrationEntity> findTopWaitlistedByEventId(String eventId);

    Optional<EventRegistrationEntity> findByIdAndUserId(String id, String userId);
    
    Optional<EventRegistrationEntity> findByRegistrationCode(String registrationCode);
    
    @Query("""
            SELECT COUNT(r)
            FROM EventRegistrationEntity r
            WHERE r.registrationStatus = :status
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
            AND r.registrationStatus = :status
            AND r.createdAt BETWEEN :start AND :end
            AND r.isDeleted = false
            """)
    long countByOrganizerIdAndStatusAndCreatedAtBetween(
            @Param("organizerId") String organizerId,
            @Param("status") RegistrationStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    Optional<EventRegistrationEntity> findByIdAndUserIdAndIsDeletedFalse(String id, String userId);

    Optional<EventRegistrationEntity> findByRegistrationCodeAndIsDeletedFalse(String registrationCode);

        @Query("""
                SELECT COUNT(r) FROM EventRegistrationEntity r
                WHERE r.user.id = :userId
                    AND r.registrationStatus = :status
                    AND r.checkInTime IS NULL
                    AND r.isDeleted = false
                    AND r.event.isDeleted = false
                """)
        long countByUserIdAndRegistrationStatusAndCheckInTimeIsNull(
                        @Param("userId") String userId,
                        @Param("status") RegistrationStatus status);

        @Query("""
                SELECT COUNT(r) FROM EventRegistrationEntity r
                WHERE r.user.id = :userId
                    AND r.registrationStatus = :status
                    AND r.isDeleted = false
                    AND r.event.isDeleted = false
                """)
        long countByUserIdAndRegistrationStatus(
                        @Param("userId") String userId,
                        @Param("status") RegistrationStatus status);
}
