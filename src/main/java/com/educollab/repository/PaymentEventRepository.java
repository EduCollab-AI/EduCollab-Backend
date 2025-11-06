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
    
    @Query("SELECT COUNT(pe) > 0 FROM PaymentEvent pe WHERE pe.studentId = :studentId AND pe.item = :item AND pe.amount = :amount AND pe.dueDate = :dueDate")
    boolean existsByStudentIdAndItemAndAmountAndDueDate(
        @Param("studentId") UUID studentId,
        @Param("item") String item,
        @Param("amount") java.math.BigDecimal amount,
        @Param("dueDate") LocalDate dueDate
    );
    
    List<PaymentEvent> findByStudentIdOrderByDueDateDesc(UUID studentId);
}

