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
    public Map<String, Object> getPaymentEvents(String studentIdStr,
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
            
            // Fetch payment schedules once (used for formatting and event generation)
            List<PaymentSchedule> paymentSchedules = paymentScheduleRepository.findByStudentId(studentId);
            System.out.println("✅ Found " + paymentSchedules.size() + " payment schedule(s)");
            
            // Step 1: Query payment_events by studentId and dueDate range
            List<PaymentEvent> existingEvents = paymentEventRepository.findByStudentIdAndDueDateBetweenOrderByDueDateAsc(
                studentId, startDate, endDate
            );
            
            System.out.println("✅ Found " + existingEvents.size() + " existing payment event(s)");
            
            // Step 2: If count > maximumCount, return immediately
            if (maximumCount != null && existingEvents.size() > maximumCount) {
                System.out.println("⚠️ Event count (" + existingEvents.size() + ") exceeds maximumCount (" + maximumCount + "), returning existing events");
                return buildPaymentResponse(
                    formatPaymentSchedules(paymentSchedules),
                    formatPaymentEvents(existingEvents.subList(0, maximumCount))
                );
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
                    existingEvents = paymentEventRepository.findByStudentIdAndDueDateBetweenOrderByDueDateAsc(
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
            
            return buildPaymentResponse(
                formatPaymentSchedules(paymentSchedules),
                formatPaymentEvents(existingEvents)
            );
            
        } catch (Exception e) {
            System.err.println("❌ Error getting payment events: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get payment events: " + e.getMessage(), e);
        }
    }
    
    private Map<String, Object> buildPaymentResponse(List<Map<String, Object>> schedules,
                                                     List<Map<String, Object>> events) {
        Map<String, Object> response = new HashMap<>();
        response.put("payment_schedules", schedules);
        response.put("payment_events", events);
        return response;
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
            // Use payment_schedule_id and dueDate as unique key
            if (existing.getPaymentScheduleId() != null) {
                String key = existing.getPaymentScheduleId() + "_" + existing.getDueDate();
                existingEventKeys.add(key);
            }
        }
        
        // Calculate how many events we can still generate
        int existingCount = paymentEventRepository.findByStudentIdAndDueDateBetweenOrderByDueDateAsc(studentId, startDate, endDate).size();
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
                
                // Check if this event already exists (using payment_schedule_id and dueDate as unique key)
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
    
    private List<Map<String, Object>> formatPaymentSchedules(List<PaymentSchedule> schedules) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (PaymentSchedule schedule : schedules) {
            Map<String, Object> scheduleData = new HashMap<>();
            scheduleData.put("id", schedule.getId() != null ? schedule.getId().toString() : null);
            scheduleData.put("billing_rrule", schedule.getBillingRule());
            scheduleData.put("startDate", schedule.getStartDate() != null ? schedule.getStartDate().toString() : null);
            scheduleData.put("amount", schedule.getAmount());
            scheduleData.put("item", schedule.getItem());
            scheduleData.put("note", schedule.getNote());
            result.add(scheduleData);
        }
        return result;
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
        
        // Parse RRULE format (e.g., "FREQ=MONTHLY;BYMONTHDAY=5" or "FREQ=WEEKLY;INTERVAL=2;BYDAY=TU")
        if (billingRule.toUpperCase().startsWith("FREQ=")) {
            String[] parts = billingRule.toUpperCase().split(";");
            String freq = null;
            Integer interval = 1; // Default interval is 1
            Integer byMonthDay = null;
            String byDay = null;
            
            for (String part : parts) {
                if (part.startsWith("FREQ=")) {
                    freq = part.substring(5);
                } else if (part.startsWith("INTERVAL=")) {
                    try {
                        interval = Integer.parseInt(part.substring(9));
                    } catch (NumberFormatException e) {
                        System.err.println("⚠️ Invalid INTERVAL value in RRULE: " + part + ", using default 1");
                        interval = 1;
                    }
                } else if (part.startsWith("BYMONTHDAY=")) {
                    byMonthDay = Integer.parseInt(part.substring(11));
                } else if (part.startsWith("BYDAY=")) {
                    byDay = part.substring(6);
                }
            }
            
            if (freq == null) {
                freq = "MONTHLY"; // Default for payments
            }
            
            System.out.println("📅 Parsed RRULE - FREQ: " + freq + ", INTERVAL: " + interval + ", BYDAY: " + byDay + ", BYMONTHDAY: " + byMonthDay);
            
            LocalDate currentDate = effectiveStartDate;
            
            switch (freq) {
                case "DAILY":
                    while (!currentDate.isAfter(endDate)) {
                        if (!currentDate.isBefore(scheduleStartDate)) {
                            dueDates.add(currentDate);
                        }
                        currentDate = currentDate.plusDays(interval);
                    }
                    break;
                    
                case "WEEKLY":
                    DayOfWeek targetDay = parseDayOfWeekString(byDay);
                    if (targetDay == null) {
                        // If no BYDAY specified, use the start date's day of week
                        targetDay = scheduleStartDate.getDayOfWeek();
                        System.out.println("ℹ️ No BYDAY specified in RRULE, using schedule start date day: " + targetDay);
                    } else {
                        System.out.println("✅ Target day from BYDAY: " + targetDay);
                    }
                    
                    // Find first occurrence of target day on or after scheduleStartDate
                    currentDate = scheduleStartDate;
                    int daysToAdd = (targetDay.getValue() + 7 - scheduleStartDate.getDayOfWeek().getValue()) % 7;
                    if (daysToAdd > 0) {
                        currentDate = scheduleStartDate.plusDays(daysToAdd);
                    }
                    // If scheduleStartDate is already the target day, currentDate = scheduleStartDate (daysToAdd = 0)
                    
                    // Ensure we start from effectiveStartDate if it's later than the first occurrence
                    if (currentDate.isBefore(effectiveStartDate)) {
                        // Find next occurrence of target day on or after effectiveStartDate
                        int daysFromEffectiveStart = (targetDay.getValue() + 7 - effectiveStartDate.getDayOfWeek().getValue()) % 7;
                        currentDate = effectiveStartDate.plusDays(daysFromEffectiveStart);
                    }
                    
                    // Generate events starting from currentDate, incrementing by interval weeks
                    while (!currentDate.isAfter(endDate)) {
                        if (!currentDate.isBefore(scheduleStartDate)) {
                            dueDates.add(currentDate);
                        }
                        // Use interval to determine weeks to skip (e.g., INTERVAL=2 means every 2 weeks)
                        currentDate = currentDate.plusWeeks(interval);
                    }
                    break;
                    
                case "MONTHLY":
                    if (byMonthDay != null) {
                        // Monthly on specific day (e.g., 5th of each month)
                        currentDate = LocalDate.of(effectiveStartDate.getYear(), effectiveStartDate.getMonth(), 
                                                  Math.min(byMonthDay, effectiveStartDate.lengthOfMonth()));
                        
                        if (currentDate.isBefore(effectiveStartDate)) {
                            currentDate = currentDate.plusMonths(interval);
                            currentDate = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), 
                                                      Math.min(byMonthDay, currentDate.lengthOfMonth()));
                        }
                        
                        while (!currentDate.isAfter(endDate)) {
                            if (!currentDate.isBefore(scheduleStartDate)) {
                                dueDates.add(currentDate);
                            }
                            currentDate = currentDate.plusMonths(interval);
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
                            currentDate = currentDate.plusMonths(interval);
                        }
                        while (!currentDate.isAfter(endDate)) {
                            if (!currentDate.isBefore(scheduleStartDate)) {
                                dueDates.add(currentDate);
                            }
                            currentDate = currentDate.plusMonths(interval);
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
    
    @Transactional
    public Map<String, Object> updatePaymentEventStatus(String paymentEventIdStr, String status) {
        try {
            System.out.println("========================================");
            System.out.println("💰 Updating payment event status:");
            System.out.println("Payment Event ID: " + paymentEventIdStr);
            System.out.println("New Status: " + status);
            System.out.println("========================================");
            
            UUID paymentEventId = UUID.fromString(paymentEventIdStr);
            
            // Find the payment event
            PaymentEvent paymentEvent = paymentEventRepository.findById(paymentEventId)
                .orElseThrow(() -> new RuntimeException("Payment event not found with ID: " + paymentEventIdStr));
            
            System.out.println("✅ Found payment event: " + paymentEvent.getId() + ", Current status: " + paymentEvent.getStatus());
            
            // Validate status transition
            if (!"pending".equalsIgnoreCase(paymentEvent.getStatus())) {
                throw new RuntimeException("Payment event status must be 'pending' to update to 'paid'. Current status: " + paymentEvent.getStatus());
            }
            
            if (!"paid".equalsIgnoreCase(status)) {
                throw new RuntimeException("Invalid status. Only 'paid' status is allowed. Provided: " + status);
            }
            
            // Update status to paid
            paymentEvent.setStatus("paid");
            paymentEvent.setPaidDate(LocalDate.now());
            paymentEvent.setUpdatedAt(LocalDateTime.now());
            
            // Save the updated event
            PaymentEvent updatedEvent = paymentEventRepository.save(paymentEvent);
            
            System.out.println("✅ Payment event updated successfully");
            System.out.println("========================================");
            
            // Format and return the updated event
            return formatSinglePaymentEvent(updatedEvent);
            
        } catch (Exception e) {
            System.err.println("❌ Error updating payment event status: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update payment event status: " + e.getMessage(), e);
        }
    }
    
    /**
     * Format a single payment event for response
     */
    private Map<String, Object> formatSinglePaymentEvent(PaymentEvent event) {
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
        
        return eventData;
    }
    
    @Transactional
    public Map<String, Object> deletePaymentSchedule(String studentIdStr, String scheduleIdStr) {
        try {
            System.out.println("========================================");
            System.out.println("🗑️ Deleting payment schedule:");
            System.out.println("Student ID: " + studentIdStr);
            System.out.println("Schedule ID: " + scheduleIdStr);
            System.out.println("========================================");
            
            UUID studentId = UUID.fromString(studentIdStr);
            UUID scheduleId = UUID.fromString(scheduleIdStr);
            
            // Validate student exists
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentIdStr));
            System.out.println("✅ Student validated: " + student.getName());
            
            // Validate payment schedule exists and belongs to student
            PaymentSchedule schedule = paymentScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Payment schedule not found with ID: " + scheduleIdStr));
            
            if (!schedule.getStudentId().equals(studentId)) {
                throw new RuntimeException("Payment schedule does not belong to the specified student");
            }
            
            // Delete future payment events generated from this schedule (keep past/history)
            LocalDate today = LocalDate.now();
            List<PaymentEvent> futureEvents = paymentEventRepository.findByPaymentScheduleIdAndDueDateAfter(scheduleId, today);
            int futureEventsDeleted = 0;
            if (!futureEvents.isEmpty()) {
                paymentEventRepository.deleteAll(futureEvents);
                futureEventsDeleted = futureEvents.size();
                System.out.println("✅ Deleted " + futureEventsDeleted + " future payment event(s) linked to schedule");
            } else {
                System.out.println("ℹ️ No future payment events found for schedule");
            }
            
            // Delete schedule (existing past payment events remain for record keeping)
            paymentScheduleRepository.delete(schedule);
            System.out.println("✅ Payment schedule deleted successfully");
            System.out.println("========================================");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment schedule deleted successfully");
            
            Map<String, Object> data = new HashMap<>();
            data.put("paymentScheduleId", scheduleId.toString());
            data.put("futureEventsDeleted", futureEventsDeleted);
            response.put("data", data);
            
            return response;
            
        } catch (Exception e) {
            System.err.println("❌ Error deleting payment schedule: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete payment schedule: " + e.getMessage());
            return errorResponse;
        }
    }
    
    @Transactional
    public Map<String, Object> deletePaymentEvent(String studentIdStr, String paymentEventIdStr) {
        try {
            System.out.println("========================================");
            System.out.println("🗑️ Deleting payment event:");
            System.out.println("Student ID: " + studentIdStr);
            System.out.println("Payment Event ID: " + paymentEventIdStr);
            System.out.println("========================================");
            
            UUID studentId = UUID.fromString(studentIdStr);
            UUID paymentEventId = UUID.fromString(paymentEventIdStr);
            
            // Validate student exists
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentIdStr));
            System.out.println("✅ Student validated: " + student.getName());
            
            // Validate payment event exists and belongs to student
            PaymentEvent paymentEvent = paymentEventRepository.findById(paymentEventId)
                .orElseThrow(() -> new RuntimeException("Payment event not found with ID: " + paymentEventIdStr));
            
            if (!paymentEvent.getStudentId().equals(studentId)) {
                throw new RuntimeException("Payment event does not belong to the specified student");
            }
            
            // Delete payment event
            paymentEventRepository.delete(paymentEvent);
            System.out.println("✅ Payment event deleted successfully");
            System.out.println("========================================");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment event deleted successfully");
            
            Map<String, Object> data = new HashMap<>();
            data.put("paymentEventId", paymentEventId.toString());
            response.put("data", data);
            
            return response;
            
        } catch (Exception e) {
            System.err.println("❌ Error deleting payment event: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete payment event: " + e.getMessage());
            return errorResponse;
        }
    }
}

