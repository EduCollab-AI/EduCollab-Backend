package com.educollab.service;

import com.educollab.model.Schedule;
import com.educollab.model.ScheduleException;
import com.educollab.repository.ScheduleExceptionRepository;
import com.educollab.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ScheduleService {
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private ScheduleExceptionRepository scheduleExceptionRepository;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;
    
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createScheduleException(Map<String, Object> request) {
        try {
            String scheduleIdStr = (String) request.get("schedule_id");
            if (scheduleIdStr == null || scheduleIdStr.isEmpty()) {
                throw new RuntimeException("schedule_id is required");
            }
            UUID scheduleId = UUID.fromString(scheduleIdStr);
            
            Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleIdStr));
            
            LocalDate baseOriginalDate = schedule.getStartDate();
            LocalTime baseOriginalStartTime = schedule.getStartTime();
            Long baseDuration = schedule.getDurationMinutes();
            
            final LocalDate originalDate = request.get("original_date") != null
                ? LocalDate.parse(request.get("original_date").toString(), DATE_FORMAT)
                : baseOriginalDate;
            
            final LocalTime originalStartTime = request.get("original_start_time") != null
                ? LocalTime.parse(request.get("original_start_time").toString(), TIME_FORMAT)
                : baseOriginalStartTime;
            
            Boolean isCancelled = request.get("is_cancelled") != null ? (Boolean) request.get("is_cancelled") : false;
            
            LocalDate newDate = null;
            if (request.get("new_date") != null && !request.get("new_date").toString().isEmpty()) {
                newDate = LocalDate.parse(request.get("new_date").toString(), DATE_FORMAT);
            }
            
            LocalTime newStartTime = null;
            if (request.get("new_start_time") != null && !request.get("new_start_time").toString().isEmpty()) {
                newStartTime = LocalTime.parse(request.get("new_start_time").toString(), TIME_FORMAT);
            }
            
            Long newDurationMinutes = null;
            if (request.get("new_duration_minutes") != null) {
                Object durationObj = request.get("new_duration_minutes");
                if (durationObj instanceof Number) {
                    newDurationMinutes = ((Number) durationObj).longValue();
                } else if (!durationObj.toString().isEmpty()) {
                    newDurationMinutes = Long.parseLong(durationObj.toString());
                }
            }
            
            boolean hasMeaningfulChange = Boolean.TRUE.equals(isCancelled);
            if (newDate != null && !newDate.equals(originalDate)) {
                hasMeaningfulChange = true;
            }
            if (newStartTime != null && !newStartTime.equals(originalStartTime)) {
                hasMeaningfulChange = true;
            }
            if (newDurationMinutes != null && !newDurationMinutes.equals(baseDuration)) {
                hasMeaningfulChange = true;
            }
            
            if (!hasMeaningfulChange) {
                throw new RuntimeException("No changes detected. Provide updated fields or set is_cancelled to true.");
            }
            
            Optional<ScheduleException> existingOpt = scheduleExceptionRepository
                .findByScheduleIdAndOriginalDateAndOriginalStartTime(scheduleId, originalDate, originalStartTime);
            ScheduleException exception = existingOpt.orElseGet(() -> new ScheduleException(scheduleId, originalDate, originalStartTime));
            
            exception.setIsCancelled(isCancelled);
            exception.setNewDate(newDate);
            exception.setNewStartTime(newStartTime);
            exception.setNewDurationMinutes(newDurationMinutes);
            if (exception.getCreatedAt() == null) {
                exception.setCreatedAt(java.time.LocalDateTime.now());
            }
            
            ScheduleException savedException = scheduleExceptionRepository.save(exception);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Schedule exception recorded successfully");
            
            Map<String, Object> data = new HashMap<>();
            data.put("scheduleExceptionId", savedException.getId().toString());
            data.put("scheduleId", savedException.getScheduleId().toString());
            data.put("originalDate", savedException.getOriginalDate().toString());
            data.put("originalStartTime", savedException.getOriginalStartTime().toString());
            data.put("isCancelled", savedException.getIsCancelled());
            data.put("newDate", savedException.getNewDate() != null ? savedException.getNewDate().toString() : null);
            data.put("newStartTime", savedException.getNewStartTime() != null ? savedException.getNewStartTime().toString() : null);
            data.put("newDurationMinutes", savedException.getNewDurationMinutes());
            response.put("data", data);
            
            return response;
            
        } catch (Exception e) {
            System.err.println("❌ Error creating schedule exception: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create schedule exception: " + e.getMessage(), e);
        }
    }
}
