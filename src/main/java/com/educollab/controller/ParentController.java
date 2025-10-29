package com.educollab.controller;

import com.educollab.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/parent")
@CrossOrigin(origins = "*")
public class ParentController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/children")
    public Map<String, Object> addChild(@RequestBody Map<String, Object> request) {
        System.out.println("Add child endpoint accessed with data: " + request);
        return authService.addChild(request);
    }
}

