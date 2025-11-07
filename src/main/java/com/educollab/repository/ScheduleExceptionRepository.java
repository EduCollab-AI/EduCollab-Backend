package com.educollab.repository;

import com.educollab.model.ScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, UUID> {
    Optional<ScheduleException> findByScheduleIdAndOriginalDateAndOriginalStartTime(UUID scheduleId,
                                                                                     LocalDate originalDate,
                                                                                     LocalTime originalStartTime);
    List<ScheduleException> findByScheduleIdIn(Collection<UUID> scheduleIds);
}
