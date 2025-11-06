package com.educollab.repository;

import com.educollab.model.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    @Query("SELECT pe FROM PaymentEvent pe WHERE pe.studentId = :studentId AND pe.dueDate >= :startDate AND pe.dueDate <= :endDate ORDER BY pe.dueDate ASC")
    List<PaymentEvent> findByStudentIdAndDueDateRange(
        @Param("studentId") UUID studentId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    boolean existsByStudentIdAndPaymentScheduleIdAndDueDate(
        UUID studentId,
        UUID paymentScheduleId,
        LocalDate dueDate
    );
    
    List<PaymentEvent> findByStudentIdOrderByDueDateDesc(UUID studentId);
}

