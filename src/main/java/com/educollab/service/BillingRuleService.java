package com.educollab.service;

import com.educollab.model.PaymentSchedule;
import com.educollab.model.Student;
import com.educollab.repository.PaymentScheduleRepository;
import com.educollab.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BillingRuleService {
    
    @Autowired
    private PaymentScheduleRepository paymentScheduleRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createBillingRule(Map<String, Object> request) {
        try {
            System.out.println("========================================");
            System.out.println("📅 Creating new billing rule:");
            System.out.println("Request: " + request);
            System.out.println("========================================");
            
            // Extract billing rule details
            String studentIdStr = (String) request.get("studentId");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String startDateStr = (String) request.get("startDate");
            String billingRule = (String) request.get("billingRrule"); // Note: using "billingRrule" as in request
            String item = request.get("item") != null ? (String) request.get("item") : null;
            String note = request.get("note") != null ? (String) request.get("note") : null;
            
            System.out.println("Student ID: " + studentIdStr);
            System.out.println("Amount: " + amount);
            System.out.println("Start Date: " + startDateStr);
            System.out.println("Billing Rule: " + billingRule);
            System.out.println("Item: " + item);
            System.out.println("Note: " + note);
            
            // Validate required fields
            if (studentIdStr == null || studentIdStr.isEmpty()) {
                throw new RuntimeException("studentId is required");
            }
            if (billingRule == null || billingRule.isEmpty()) {
                throw new RuntimeException("billingRrule is required");
            }
            if (startDateStr == null || startDateStr.isEmpty()) {
                throw new RuntimeException("startDate is required");
            }
            
            UUID studentId = UUID.fromString(studentIdStr);
            LocalDate startDate = LocalDate.parse(startDateStr);
            
            // Validate student exists
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentIdStr));
            
            System.out.println("✅ Student validated: " + student.getName());
            
            // Create billing rule record
            PaymentSchedule paymentSchedule = new PaymentSchedule();
            paymentSchedule.setStudentId(studentId);
            paymentSchedule.setBillingRule(billingRule);
            paymentSchedule.setAmount(amount);
            paymentSchedule.setStartDate(startDate);
            paymentSchedule.setItem(item);
            paymentSchedule.setNote(note);
            
            PaymentSchedule savedSchedule = paymentScheduleRepository.save(paymentSchedule);
            
            System.out.println("✅ Billing rule saved with ID: " + savedSchedule.getId());
            System.out.println("========================================");
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Billing rule created successfully");
            
            Map<String, Object> data = new HashMap<>();
            data.put("scheduleId", savedSchedule.getId().toString());
            data.put("billingRule", savedSchedule.getBillingRule());
            data.put("startDate", savedSchedule.getStartDate().toString());
            data.put("amount", savedSchedule.getAmount());
            
            response.put("data", data);
            
            return response;
            
        } catch (Exception e) {
            System.err.println("❌ Error creating billing rule: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create billing rule: " + e.getMessage());
            
            return errorResponse;
        }
    }
}

