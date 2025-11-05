package com.educollab.controller;

import com.educollab.service.ClassScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/class")
@CrossOrigin(origins = "*")
public class ClassScheduleController {
    
    @Autowired
    private ClassScheduleService classScheduleService;
    
    @GetMapping("/schedules")
    public ResponseEntity<Map<String, Object>> getSchedules(
            @RequestParam String studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer maximumCount) {
        
        System.out.println("Get schedules endpoint accessed");
        System.out.println("Query params - studentId: " + studentId + ", startDate: " + startDate + ", endDate: " + endDate + ", maximumCount: " + maximumCount);
        
        if (studentId == null || studentId.isEmpty()) {
            throw new RuntimeException("studentId is required");
        }
        
        // Set default dates if not provided
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = LocalDate.now().plusMonths(3); // Default 3 months ahead
        }
        
        Map<String, Object> result = classScheduleService.getClassSchedules(
            studentId, 
            startDate, 
            endDate, 
            maximumCount
        );
        
        return ResponseEntity.ok(result);
    }
}
