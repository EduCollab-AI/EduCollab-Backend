package com.educollab.service;

import com.educollab.model.Course;
import com.educollab.model.Schedule;
import com.educollab.repository.CourseRepository;
import com.educollab.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CourseService {
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createCourse(Map<String, Object> request) {
        try {
            System.out.println("========================================");
            System.out.println("📚 Creating new course:");
            System.out.println("Request: " + request);
            System.out.println("========================================");
            
            // Extract course details
            String courseName = (String) request.get("courseName");
            String teacherName = (String) request.get("teacherName");
            String location = (String) request.get("location");
            Integer totalSessions = ((Number) request.get("totalSessions")).intValue();
            String courseStartDateStr = (String) request.get("courseStartDate");
            
            // Optional fields
            String description = request.get("description") != null ? (String) request.get("description") : null;
            Integer maxStudents = request.get("maxStudents") != null ? ((Number) request.get("maxStudents")).intValue() : null;
            
            System.out.println("Course Name: " + courseName);
            System.out.println("Teacher Name: " + teacherName);
            System.out.println("Location: " + location);
            System.out.println("Total Sessions: " + totalSessions);
            System.out.println("Course Start Date: " + courseStartDateStr);
            System.out.println("Description: " + description);
            
            // Parse course start date
            LocalDate courseStartDate = LocalDate.parse(courseStartDateStr);
            
            // Extract schedule array
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scheduleArray = (List<Map<String, Object>>) request.get("schedule");
            
            if (scheduleArray == null || scheduleArray.isEmpty()) {
                throw new RuntimeException("Schedule array cannot be empty");
            }
            
            System.out.println("Number of schedule entries: " + scheduleArray.size());
            
            // Step 1: Create Course entity
            Course course = new Course();
            course.setName(courseName);
            course.setTeacherName(teacherName);
            course.setLocation(location);
            course.setTotalSessions(totalSessions);
            course.setDescription(description);
            course.setMaxStudents(maxStudents);
            course.setCreatedAt(LocalDateTime.now());
            course.setUpdatedAt(LocalDateTime.now());
            
            // Step 2: Save course to get the course_id
            Course savedCourse = courseRepository.save(course);
            UUID courseId = savedCourse.getId();
            
            System.out.println("✅ Course saved with ID: " + courseId);
            
            // Step 3: Process schedule array
            List<Schedule> schedules = new ArrayList<>();
            
            for (Map<String, Object> scheduleEntry : scheduleArray) {
                String dayOfWeek = (String) scheduleEntry.get("dayOfWeek");
                String startTimeStr = (String) scheduleEntry.get("startTime");
                String endTimeStr = (String) scheduleEntry.get("endTime");
                
                System.out.println("Processing schedule entry:");
                System.out.println("  Day of Week: " + dayOfWeek);
                System.out.println("  Start Time: " + startTimeStr);
                System.out.println("  End Time: " + endTimeStr);
                
                // Parse times
                LocalTime startTime = LocalTime.parse(startTimeStr);
                LocalTime endTime = LocalTime.parse(endTimeStr);
                
                // Calculate duration in minutes
                Duration duration = Duration.between(startTime, endTime);
                Long durationMinutes = duration.toMinutes();
                
                System.out.println("  Duration: " + durationMinutes + " minutes");
                
                // Calculate first valid date for this day of week
                LocalDate firstValidDate = calculateFirstValidDate(courseStartDate, dayOfWeek);
                System.out.println("  First Valid Date: " + firstValidDate);
                
                // Create schedule entity
                Schedule schedule = new Schedule();
                schedule.setCourseId(courseId);
                schedule.setDayOfWeek(dayOfWeek);
                schedule.setStartTime(startTime);
                schedule.setStartDate(firstValidDate);
                schedule.setDurationMinutes(durationMinutes);
                schedule.setRecurrenceRule("weekly");
                schedule.setCreatedAt(LocalDateTime.now());
                schedule.setUpdatedAt(LocalDateTime.now());
                
                schedules.add(schedule);
            }
            
            // Step 4: Save all schedules
            List<Schedule> savedSchedules = scheduleRepository.saveAll(schedules);
            
            System.out.println("✅ Saved " + savedSchedules.size() + " schedule entries");
            System.out.println("========================================");
            
            // Step 5: Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Course created successfully");
            
            Map<String, Object> data = new HashMap<>();
            data.put("courseId", courseId.toString());
            data.put("schedulesCreated", savedSchedules.size());
            
            response.put("data", data);
            
            return response;
            
        } catch (Exception e) {
            System.err.println("❌ Error creating course: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create course: " + e.getMessage());
            
            return errorResponse;
        }
    }
    
    /**
     * Calculate the first valid date for a given day of week on or after the start date
     */
    private LocalDate calculateFirstValidDate(LocalDate startDate, String dayOfWeek) {
        // Map day name to DayOfWeek enum
        DayOfWeek targetDay = parseDayOfWeek(dayOfWeek);
        
        // Get the day of week for the start date
        DayOfWeek startDayOfWeek = startDate.getDayOfWeek();
        
        // Calculate days to add
        int daysToAdd = 0;
        if (targetDay.getValue() >= startDayOfWeek.getValue()) {
            // Target day is in the same week or later
            daysToAdd = targetDay.getValue() - startDayOfWeek.getValue();
        } else {
            // Target day is in the next week
            daysToAdd = 7 - (startDayOfWeek.getValue() - targetDay.getValue());
        }
        
        return startDate.plusDays(daysToAdd);
    }
    
    /**
     * Parse day of week string to DayOfWeek enum
     */
    private DayOfWeek parseDayOfWeek(String dayOfWeek) {
        // Handle common variations
        String dayLower = dayOfWeek.toLowerCase();
        
        switch (dayLower) {
            case "monday":
            case "mon":
                return DayOfWeek.MONDAY;
            case "tuesday":
            case "tue":
            case "tues":
                return DayOfWeek.TUESDAY;
            case "wednesday":
            case "wed":
                return DayOfWeek.WEDNESDAY;
            case "thursday":
            case "thu":
            case "thurs":
                return DayOfWeek.THURSDAY;
            case "friday":
            case "fri":
                return DayOfWeek.FRIDAY;
            case "saturday":
            case "sat":
                return DayOfWeek.SATURDAY;
            case "sunday":
            case "sun":
                return DayOfWeek.SUNDAY;
            default:
                throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
        }
    }
}

