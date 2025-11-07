package com.educollab.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedule_exceptions")
public class ScheduleException {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;
    
    @Column(name = "original_date", nullable = false)
    private LocalDate originalDate;
    
    @Column(name = "original_start_time", nullable = false)
    private LocalTime originalStartTime;
    
    @Column(name = "is_cancelled")
    private Boolean isCancelled;
    
    @Column(name = "new_date")
    private LocalDate newDate;
    
    @Column(name = "new_start_time")
    private LocalTime newStartTime;
    
    @Column(name = "new_duration_minutes")
    private Long newDurationMinutes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public ScheduleException() {}
    
    public ScheduleException(UUID scheduleId, LocalDate originalDate, LocalTime originalStartTime) {
        this.scheduleId = scheduleId;
        this.originalDate = originalDate;
        this.originalStartTime = originalStartTime;
        this.createdAt = LocalDateTime.now();
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getScheduleId() {
        return scheduleId;
    }
    
    public void setScheduleId(UUID scheduleId) {
        this.scheduleId = scheduleId;
    }
    
    public LocalDate getOriginalDate() {
        return originalDate;
    }
    
    public void setOriginalDate(LocalDate originalDate) {
        this.originalDate = originalDate;
    }
    
    public LocalTime getOriginalStartTime() {
        return originalStartTime;
    }
    
    public void setOriginalStartTime(LocalTime originalStartTime) {
        this.originalStartTime = originalStartTime;
    }
    
    public Boolean getIsCancelled() {
        return isCancelled;
    }
    
    public void setIsCancelled(Boolean cancelled) {
        isCancelled = cancelled;
    }
    
    public LocalDate getNewDate() {
        return newDate;
    }
    
    public void setNewDate(LocalDate newDate) {
        this.newDate = newDate;
    }
    
    public LocalTime getNewStartTime() {
        return newStartTime;
    }
    
    public void setNewStartTime(LocalTime newStartTime) {
        this.newStartTime = newStartTime;
    }
    
    public Long getNewDurationMinutes() {
        return newDurationMinutes;
    }
    
    public void setNewDurationMinutes(Long newDurationMinutes) {
        this.newDurationMinutes = newDurationMinutes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
