package com.educollab.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "institution_id")
    private Integer institutionId;
    
    @Column(name = "parent_email")
    private String parentEmail;
    
    @Column(name = "parent_phone")
    private String parentPhone;
    
    @Column(name = "birthdate")
    private LocalDate birthDate;
    
    @Column(name = "associated_parent_id")
    private String associatedParentId;
    
    @Column(name = "is_associated", nullable = false)
    private Boolean isAssociated;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public Student() {}
    
    public Student(String name, String associatedParentId, Boolean isAssociated, LocalDate birthDate) {
        this.name = name;
        this.associatedParentId = associatedParentId;
        this.isAssociated = isAssociated;
        this.birthDate = birthDate;
        this.institutionId = null;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getInstitutionId() {
        return institutionId;
    }
    
    public void setInstitutionId(Integer institutionId) {
        this.institutionId = institutionId;
    }
    
    public String getParentEmail() {
        return parentEmail;
    }
    
    public void setParentEmail(String parentEmail) {
        this.parentEmail = parentEmail;
    }
    
    public String getParentPhone() {
        return parentPhone;
    }
    
    public void setParentPhone(String parentPhone) {
        this.parentPhone = parentPhone;
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    
    public String getAssociatedParentId() {
        return associatedParentId;
    }
    
    public void setAssociatedParentId(String associatedParentId) {
        this.associatedParentId = associatedParentId;
    }
    
    public Boolean getIsAssociated() {
        return isAssociated;
    }
    
    public void setIsAssociated(Boolean isAssociated) {
        this.isAssociated = isAssociated;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

