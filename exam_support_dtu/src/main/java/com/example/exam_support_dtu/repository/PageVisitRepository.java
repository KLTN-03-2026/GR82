package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.PageVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface PageVisitRepository extends JpaRepository<PageVisit, Long> {

    long countByVisitTimeAfter(LocalDateTime time);
    long countByVisitTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT CAST(pv.visitTime AS date) as visitDate, COUNT(pv) as visitCount " +
           "FROM PageVisit pv " +
           "WHERE pv.visitTime >= :since " +
           "GROUP BY CAST(pv.visitTime AS date) " +
           "ORDER BY CAST(pv.visitTime AS date) ASC")
    List<Map<String, Object>> countVisitsByDay(@Param("since") LocalDateTime since);
}
