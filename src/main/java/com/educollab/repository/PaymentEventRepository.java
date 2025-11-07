package com.educollab.repository;

import com.educollab.model.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
    List<PaymentEvent> findByStudentIdAndDueDateBetweenOrderByDueDateAsc(
        UUID studentId, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    boolean existsByStudentIdAndPaymentScheduleIdAndDueDate(
        UUID studentId,
        UUID paymentScheduleId,
        LocalDate dueDate
    );
    
    List<PaymentEvent> findByStudentIdOrderByDueDateDesc(UUID studentId);
    
    List<PaymentEvent> findByPaymentScheduleIdAndDueDateAfter(UUID paymentScheduleId, LocalDate date);
}

