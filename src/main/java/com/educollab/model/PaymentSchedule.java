package com.educollab.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payment_schedules")
public class PaymentSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "student_id", nullable = false)
    private UUID studentId;
    
    @Column(name = "billing_rrule", nullable = false)
    private String billingRule;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "item")
    private String item;
    
    @Column(name = "note")
    private String note;
    
    // Constructors
    public PaymentSchedule() {}
    
    public PaymentSchedule(UUID studentId, String billingRule, BigDecimal amount, LocalDate startDate) {
        this.studentId = studentId;
        this.billingRule = billingRule;
        this.amount = amount;
        this.startDate = startDate;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getStudentId() {
        return studentId;
    }
    
    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }
    
    public String getBillingRule() {
        return billingRule;
    }
    
    public void setBillingRule(String billingRule) {
        this.billingRule = billingRule;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public String getItem() {
        return item;
    }
    
    public void setItem(String item) {
        this.item = item;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
}

