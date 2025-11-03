package com.educollab.repository;

import com.educollab.model.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, UUID> {
    List<PaymentSchedule> findByStudentId(UUID studentId);
}

