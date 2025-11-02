package com.educollab.repository;

import com.educollab.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    List<Enrollment> findByCourseId(UUID courseId);
    List<Enrollment> findByStudentId(UUID studentId);
    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);
}

