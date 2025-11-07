package com.educollab.controller;

import com.educollab.service.PaymentQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    @Autowired
    private PaymentQueryService paymentQueryService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPaymentEvents(
            @RequestParam String studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer maximumCount) {
        
        System.out.println("Get payment events endpoint accessed");
        System.out.println("Query params - studentId: " + studentId + ", startDate: " + startDate + 
                          ", endDate: " + endDate + ", maximumCount: " + maximumCount);
        
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
        
        Map<String, Object> result = paymentQueryService.getPaymentEvents(
            studentId, 
            startDate, 
            endDate, 
            maximumCount
        );
        
        return ResponseEntity.ok(result);
    }
    
    @PutMapping("/{paymentEventId}/status")
    public ResponseEntity<Map<String, Object>> updatePaymentEventStatus(
            @PathVariable String paymentEventId,
            @RequestBody Map<String, Object> request) {
        
        System.out.println("Update payment event status endpoint accessed");
        System.out.println("Payment Event ID: " + paymentEventId);
        System.out.println("Request body: " + request);
        
        String status = request.get("status") != null ? request.get("status").toString() : null;
        
        if (status == null || status.isEmpty()) {
            throw new RuntimeException("status is required in request body");
        }
        
        Map<String, Object> result = paymentQueryService.updatePaymentEventStatus(paymentEventId, status);
        
        return ResponseEntity.ok(result);
    }
}
