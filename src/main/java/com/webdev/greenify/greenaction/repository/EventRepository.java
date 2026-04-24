package com.webdev.greenify.greenaction.repository;

import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, String>, JpaSpecificationExecutor<EventEntity> {
    long countByStatus(GreenEventStatus status);

    @Override
    @EntityGraph(attributePaths = { "images", "address", "organizer", "organizer.ngoProfile" })
    Page<EventEntity> findAll(Specification<EventEntity> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "images", "address", "organizer", "organizer.ngoProfile" })
    List<EventEntity> findByIdIn(Collection<String> ids);

    @Query("SELECT COALESCE(AVG(CAST(e.participantCount AS double)), 0.0) " +
            "FROM EventEntity e " +
            "WHERE e.eventType = :eventType " +
            "AND e.address.province = :province " +
            "AND (" +
            "  HOUR(e.startTime) BETWEEN :startHour - 2 AND :startHour + 2 " +
            "  OR HOUR(e.endTime) BETWEEN :endHour - 2 AND :endHour + 2" +
            ") " +
            "AND e.status = 'COMPLETED'")
    Double getAverageParticipantsByCriteria(
            @Param("eventType") GreenEventType eventType,
            @Param("province") String province,
            @Param("startHour") int startHour,
            @Param("endHour") int endHour);

    @Query("""
            SELECT COUNT(e)
            FROM EventEntity e
            WHERE e.status = 'COMPLETED'
            AND e.isDeleted = false
            """)
    long countCompletedEvents();

    @Query("""
            SELECT COUNT(e)
            FROM EventEntity e
            WHERE e.organizer.id = :organizerId
            AND e.createdAt BETWEEN :start AND :end
            AND e.isDeleted = false
            """)
    long countByOrganizerIdAndCreatedAtBetween(
            @Param("organizerId") String organizerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    long countByIsDeletedFalse();

    @Query("SELECT COALESCE(SUM(e.participantCount), 0) FROM EventEntity e WHERE e.status = :status AND e.isDeleted = false AND e.endTime BETWEEN :start AND :end")
    long sumParticipantCountByStatusAndEndTimeBetween(@Param("status") GreenEventStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(e.participantCount), 0) FROM EventEntity e WHERE e.organizer.id = :organizerId AND e.isDeleted = false AND e.createdAt BETWEEN :start AND :end")
    long sumParticipantCountByOrganizerIdAndCreatedAtBetween(@Param("organizerId") String organizerId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
