package com.educollab.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_enrollments")
public class Enrollment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "course_id", nullable = false)
    private UUID courseId;
    
    @Column(name = "child_id", nullable = false)
    private UUID childId; // This will map to our students.id
    
    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;
    
    @Column(name = "status")
    private String status;
    
    // Constructors
    public Enrollment() {}
    
    public Enrollment(UUID courseId, UUID childId) {
        this.courseId = courseId;
        this.childId = childId;
        this.status = "active";
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getCourseId() {
        return courseId;
    }
    
    public void setCourseId(UUID courseId) {
        this.courseId = courseId;
    }
    
    public UUID getChildId() {
        return childId;
    }
    
    public void setChildId(UUID childId) {
        this.childId = childId;
    }
    
    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }
    
    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}

