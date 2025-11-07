package com.educollab.controller;

import com.educollab.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/schedules")
@CrossOrigin(origins = "*")
public class ScheduleController {
    
    @Autowired
    private ScheduleService scheduleService;
    
    @PostMapping("/exceptions")
    public ResponseEntity<Map<String, Object>> createScheduleException(@RequestBody Map<String, Object> request) {
        System.out.println("Create schedule exception endpoint accessed with data: " + request);
        Map<String, Object> result = scheduleService.createScheduleException(request);
        return ResponseEntity.ok(result);
    }
}
