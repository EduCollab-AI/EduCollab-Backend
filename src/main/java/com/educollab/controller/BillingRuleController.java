package com.educollab.controller;

import com.educollab.service.BillingRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing-rules")
@CrossOrigin(origins = "*")
public class BillingRuleController {
    
    @Autowired
    private BillingRuleService billingRuleService;
    
    @GetMapping("/health")
    public String health() {
        System.out.println("Billing rule health endpoint accessed");
        return "Billing rule service is running";
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBillingRule(@RequestBody Map<String, Object> request) {
        System.out.println("Create billing rule endpoint accessed with data: " + request);
        Map<String, Object> result = billingRuleService.createBillingRule(request);
        
        if (result.get("success").equals(true)) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
}

