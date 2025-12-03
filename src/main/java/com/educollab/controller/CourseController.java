package com.educollab.controller;

import com.educollab.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/courses")
@CrossOrigin(origins = "*")
public class CourseController {
    
    @Autowired
    private CourseService courseService;
    
    @GetMapping("/health")
    public String health() {
        System.out.println("Course health endpoint accessed");
        return "Course service is running";
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCourse(@RequestBody Map<String, Object> request) {
        System.out.println("Create course endpoint accessed with data: " + request);
        Map<String, Object> result = courseService.createCourse(request);
        
        if (result.get("success").equals(true)) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
    
    @PostMapping("/institution")
    public ResponseEntity<Map<String, Object>> createInstitutionCourse(@RequestBody Map<String, Object> request) {
        System.out.println("Create institution course endpoint accessed with data: " + request);
        Map<String, Object> result = courseService.createInstitutionCourse(request);
        
        if (result.get("success").equals(true)) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
    
    @DeleteMapping("/{courseId}/enrollments")
    public ResponseEntity<Map<String, Object>> deleteStudentEnrollment(
            @PathVariable String courseId,
            @RequestParam String studentId) {
        System.out.println("Delete student enrollment endpoint accessed");
        System.out.println("Course ID: " + courseId);
        System.out.println("Student ID: " + studentId);
        
        if (studentId == null || studentId.isEmpty()) {
            throw new RuntimeException("studentId is required as query parameter");
        }
        
        Map<String, Object> result = courseService.deleteStudentEnrollment(studentId, courseId);
        
        if (result.get("success").equals(true)) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
}

