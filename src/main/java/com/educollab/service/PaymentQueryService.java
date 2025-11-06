package com.educollab.service;

import com.educollab.model.PaymentEvent;
import com.educollab.model.PaymentSchedule;
import com.educollab.model.Student;
import com.educollab.repository.PaymentEventRepository;
import com.educollab.repository.PaymentScheduleRepository;
import com.educollab.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentQueryService {
    
    @Autowired
    private PaymentEventRepository paymentEventRepository;
    
    @Autowired
    private PaymentScheduleRepository paymentScheduleRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Transactional
    public List<Map<String, Object>> getPaymentEvents(String studentIdStr,
                                                       LocalDate startDate,
                                                       LocalDate endDate,
                                                       Integer maximumCount) {
        try {
            System.out.println("========================================");
            System.out.println("💰 Getting payment events:");
            System.out.println("Student ID: " + studentIdStr);
            System.out.println("Start Date: " + startDate);
            System.out.println("End Date: " + endDate);
            System.out.println("Maximum Count: " + maximumCount);
            System.out.println("========================================");
            
            UUID studentId = UUID.fromString(studentIdStr);
            
            // Validate student exists
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentIdStr));
            
            System.out.println("✅ Student validated: " + student.getName());
            
            // Step 1: Query payment_events by studentId and dueDate range
            List<PaymentEvent> existingEvents = paymentEventRepository.findByStudentIdAndDueDateRange(
                studentId, startDate, endDate
            );
            
            System.out.println("✅ Found " + existingEvents.size() + " existing payment event(s)");
            
            // Step 2: If count > maximumCount, return immediately
            if (maximumCount != null && existingEvents.size() > maximumCount) {
                System.out.println("⚠️ Event count (" + existingEvents.size() + ") exceeds maximumCount (" + maximumCount + "), returning existing events");
                return formatPaymentEvents(existingEvents.subList(0, maximumCount));
            }
            
            // Step 3: Check if we need to generate new events
            boolean shouldGenerate = false;
            if (existingEvents.isEmpty()) {
                shouldGenerate = true;
                System.out.println("📝 No existing events, will generate new ones");
            } else {
                // Check if count < maximumCount AND latest event's dueDate is within endDate
                PaymentEvent latestEvent = existingEvents.get(existingEvents.size() - 1);
                if (maximumCount == null || existingEvents.size() < maximumCount) {
                    if (latestEvent.getDueDate().isBefore(endDate) || latestEvent.getDueDate().equals(endDate)) {
                        shouldGenerate = true;
                        System.out.println("📝 Event count (" + existingEvents.size() + ") < maximumCount and latest event dueDate (" + 
                                          latestEvent.getDueDate() + ") is within endDate, will generate new ones");
                    }
                }
            }
            
            if (shouldGenerate) {
                // Generate new payment events from payment_schedules
                List<PaymentSchedule> paymentSchedules = paymentScheduleRepository.findByStudentId(studentId);
                System.out.println("✅ Found " + paymentSchedules.size() + " payment schedule(s)");
                
                List<PaymentEvent> newEvents = generatePaymentEventsFromSchedules(
                    paymentSchedules, 
                    studentId, 
                    startDate, 
                    endDate, 
                    maximumCount
                );
                
                if (!newEvents.isEmpty()) {
                    System.out.println("✅ Generated " + newEvents.size() + " new payment event(s)");
                    // Save new events
                    paymentEventRepository.saveAll(newEvents);
                    
                    // Query again to get all events
                    existingEvents = paymentEventRepository.findByStudentIdAndDueDateRange(
                        studentId, startDate, endDate
                    );
                    System.out.println("✅ Total events after generation: " + existingEvents.size());
                }
            }
            
            // Apply maximumCount limit if needed
            if (maximumCount != null && existingEvents.size() > maximumCount) {
                existingEvents = existingEvents.subList(0, maximumCount);
            }
            
            System.out.println("✅ Returning " + existingEvents.size() + " payment event(s)");
            System.out.println("========================================");
            
            return formatPaymentEvents(existingEvents);
            
        } catch (Exception e) {
            System.err.println("❌ Error getting payment events: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get payment events: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate payment events from payment schedules using billing_rrule
     */
    private List<PaymentEvent> generatePaymentEventsFromSchedules(List<PaymentSchedule> schedules,
                                                                   UUID studentId,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate,
                                                                   Integer maximumCount) {
        List<PaymentEvent> newEvents = new ArrayList<>();
        Set<String> existingEventKeys = new HashSet<>();
        
        // Get existing events to avoid duplicates
        List<PaymentEvent> existingEvents = paymentEventRepository.findByStudentIdOrderByDueDateDesc(studentId);
        for (PaymentEvent existing : existingEvents) {
            String key = existing.getPaymentScheduleId() + "_" + existing.getDueDate();
            existingEventKeys.add(key);
        }
        
        // Calculate how many events we can still generate
        int existingCount = paymentEventRepository.findByStudentIdAndDueDateRange(studentId, startDate, endDate).size();
        int remainingSlots = maximumCount != null ? Math.max(0, maximumCount - existingCount) : Integer.MAX_VALUE;
        
        System.out.println("📊 Existing events in range: " + existingCount + ", Remaining slots: " + remainingSlots);
        
        for (PaymentSchedule schedule : schedules) {
            if (remainingSlots <= 0) {
                break;
            }
            
            List<LocalDate> dueDates = calculateDueDatesFromRRULE(
                schedule.getBillingRule(),
                schedule.getStartDate(),
                startDate,
                endDate
            );
            
            for (LocalDate dueDate : dueDates) {
                if (remainingSlots <= 0) {
                    break;
                }
                
                // Check if dueDate is within range
                if (dueDate.isBefore(startDate) || dueDate.isAfter(endDate)) {
                    continue;
                }
                
                // Check if this event already exists (using both cache and repository)
                String eventKey = schedule.getId() + "_" + dueDate;
                if (existingEventKeys.contains(eventKey)) {
                    continue;
                }
                
                // Double-check using repository method
                if (paymentEventRepository.existsByStudentIdAndPaymentScheduleIdAndDueDate(
                    studentId, schedule.getId(), dueDate)) {
                    existingEventKeys.add(eventKey);
                    continue;
                }
                
                // Create new payment event
                PaymentEvent event = new PaymentEvent();
                event.setStudentId(studentId);
                event.setPaymentScheduleId(schedule.getId());
                event.setItem(schedule.getItem());
                event.setAmount(schedule.getAmount());
                event.setDueDate(dueDate);
                event.setStatus("pending");
                event.setNote(schedule.getNote());
                event.setCreatedAt(LocalDateTime.now());
                event.setUpdatedAt(LocalDateTime.now());
                
                newEvents.add(event);
                existingEventKeys.add(eventKey);
                remainingSlots--;
            }
        }
        
        return newEvents;
    }
    
    /**
     * Calculate due dates from billing_rrule (RRULE format)
     */
    private List<LocalDate> calculateDueDatesFromRRULE(String billingRule,
                                                        LocalDate scheduleStartDate,
                                                        LocalDate startDate,
                                                        LocalDate endDate) {
        List<LocalDate> dueDates = new ArrayList<>();
        
        // Use effective start date (max of schedule start and requested start)
        LocalDate effectiveStartDate = scheduleStartDate.isAfter(startDate) ? scheduleStartDate : startDate;
        
        if (billingRule == null || billingRule.isEmpty()) {
            return dueDates;
        }
        
        // Parse RRULE format (e.g., "FREQ=MONTHLY;BYMONTHDAY=5")
        if (billingRule.toUpperCase().startsWith("FREQ=")) {
            String[] parts = billingRule.toUpperCase().split(";");
            String freq = null;
            Integer byMonthDay = null;
            String byDay = null;
            
            for (String part : parts) {
                if (part.startsWith("FREQ=")) {
                    freq = part.substring(5);
                } else if (part.startsWith("BYMONTHDAY=")) {
                    byMonthDay = Integer.parseInt(part.substring(11));
                } else if (part.startsWith("BYDAY=")) {
                    byDay = part.substring(6);
                }
            }
            
            if (freq == null) {
                freq = "MONTHLY"; // Default for payments
            }
            
            LocalDate currentDate = effectiveStartDate;
            
            switch (freq) {
                case "DAILY":
                    while (!currentDate.isAfter(endDate)) {
                        if (!currentDate.isBefore(scheduleStartDate)) {
                            dueDates.add(currentDate);
                        }
                        currentDate = currentDate.plusDays(1);
                    }
                    break;
                    
                case "WEEKLY":
                    DayOfWeek targetDay = parseDayOfWeekString(byDay);
                    if (targetDay == null) {
                        targetDay = scheduleStartDate.getDayOfWeek();
                    }
                    
                    // Find first occurrence
                    while (currentDate.getDayOfWeek() != targetDay && !currentDate.isAfter(endDate)) {
                        currentDate = currentDate.plusDays(1);
                    }
                    
                    while (!currentDate.isAfter(endDate)) {
                        if (!currentDate.isBefore(scheduleStartDate)) {
                            dueDates.add(currentDate);
                        }
                        currentDate = currentDate.plusWeeks(1);
                    }
                    break;
                    
                case "MONTHLY":
                    if (byMonthDay != null) {
                        // Monthly on specific day (e.g., 5th of each month)
                        currentDate = LocalDate.of(effectiveStartDate.getYear(), effectiveStartDate.getMonth(), 
                                                  Math.min(byMonthDay, effectiveStartDate.lengthOfMonth()));
                        
                        if (currentDate.isBefore(effectiveStartDate)) {
                            currentDate = currentDate.plusMonths(1);
                            currentDate = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), 
                                                      Math.min(byMonthDay, currentDate.lengthOfMonth()));
                        }
                        
                        while (!currentDate.isAfter(endDate)) {
                            if (!currentDate.isBefore(scheduleStartDate)) {
                                dueDates.add(currentDate);
                            }
                            currentDate = currentDate.plusMonths(1);
                            if (currentDate.lengthOfMonth() < byMonthDay) {
                                currentDate = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), 
                                                          currentDate.lengthOfMonth());
                            } else {
                                currentDate = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), byMonthDay);
                            }
                        }
                    } else {
                        // Monthly on same day of month
                        currentDate = scheduleStartDate.isBefore(effectiveStartDate) ? 
                                     LocalDate.of(effectiveStartDate.getYear(), effectiveStartDate.getMonth(), 
                                                 scheduleStartDate.getDayOfMonth()) : scheduleStartDate;
                        if (currentDate.isBefore(effectiveStartDate)) {
                            currentDate = currentDate.plusMonths(1);
                        }
                        while (!currentDate.isAfter(endDate)) {
                            dueDates.add(currentDate);
                            currentDate = currentDate.plusMonths(1);
                            if (currentDate.lengthOfMonth() < scheduleStartDate.getDayOfMonth()) {
                                currentDate = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), 
                                                          currentDate.lengthOfMonth());
                            }
                        }
                    }
                    break;
            }
        }
        
        return dueDates;
    }
    
    /**
     * Parse day of week from RRULE BYDAY format (e.g., "MO", "TU")
     */
    private DayOfWeek parseDayOfWeekString(String byDay) {
        if (byDay == null || byDay.isEmpty()) {
            return null;
        }
        
        String dayUpper = byDay.toUpperCase();
        switch (dayUpper) {
            case "MO":
            case "MONDAY":
                return DayOfWeek.MONDAY;
            case "TU":
            case "TUESDAY":
                return DayOfWeek.TUESDAY;
            case "WE":
            case "WEDNESDAY":
                return DayOfWeek.WEDNESDAY;
            case "TH":
            case "THURSDAY":
                return DayOfWeek.THURSDAY;
            case "FR":
            case "FRIDAY":
                return DayOfWeek.FRIDAY;
            case "SA":
            case "SATURDAY":
                return DayOfWeek.SATURDAY;
            case "SU":
            case "SUNDAY":
                return DayOfWeek.SUNDAY;
            default:
                return null;
        }
    }
    
    /**
     * Format payment events for response
     */
    private List<Map<String, Object>> formatPaymentEvents(List<PaymentEvent> events) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (PaymentEvent event : events) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("paymentEventId", event.getId().toString());
            eventData.put("item", event.getItem());
            eventData.put("amount", event.getAmount());
            
            // Map status: 'paid' -> 'PAID', others -> 'UNPAID'
            String status = "paid".equalsIgnoreCase(event.getStatus()) ? "PAID" : "UNPAID";
            eventData.put("status", status);
            
            eventData.put("dueDate", event.getDueDate().toString());
            eventData.put("paidDate", event.getPaidDate() != null ? event.getPaidDate().toString() : null);
            eventData.put("note", event.getNote());
            
            result.add(eventData);
        }
        
        return result;
    }
}

