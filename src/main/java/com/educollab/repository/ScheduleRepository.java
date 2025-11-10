package com.educollab.repository;

import com.educollab.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    List<Schedule> findByCourseId(UUID courseId);
    
    List<Schedule> findByCourseIdIn(Collection<UUID> courseIds);
}

