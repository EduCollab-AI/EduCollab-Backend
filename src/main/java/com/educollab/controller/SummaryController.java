package com.educollab.controller;

import com.educollab.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/summary")
@CrossOrigin(origins = "*")
public class SummaryController {
    
    @Autowired
    private SummaryService summaryService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStudentSummary(@RequestParam String studentId) {
        Map<String, Object> summary = summaryService.getStudentSummary(studentId);
        return ResponseEntity.ok(summary);
    }
}


