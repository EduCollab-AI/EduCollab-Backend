package com.educollab.service;

import com.educollab.model.Course;
import com.educollab.model.Enrollment;
import com.educollab.model.Schedule;
import com.educollab.model.Student;
import com.educollab.repository.CourseRepository;
import com.educollab.repository.EnrollmentRepository;
import com.educollab.repository.ScheduleRepository;
import com.educollab.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class ClassScheduleService {
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Transactional(readOnly = true)
    public Map<String, Object> getClassSchedules(String studentIdStr, 
                                                   LocalDate startDate, 
                                                   LocalDate endDate, 
                                                   Integer maximumCount) {
        try {
            System.out.println("========================================");
            System.out.println("📅 Getting class schedules:");
            System.out.println("Student ID: " + studentIdStr);
            System.out.println("Start Date: " + startDate);
            System.out.println("End Date: " + endDate);
            System.out.println("Maximum Count: " + maximumCount);
            System.out.println("========================================");
            
            // Validate student exists
            UUID studentId = UUID.fromString(studentIdStr);
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentIdStr));
            
            System.out.println("✅ Student validated: " + student.getName());
            
            // Step 1: Find all enrollments for this student
            List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
            
            if (enrollments.isEmpty()) {
                System.out.println("⚠️ No enrollments found for student");
                return buildEmptyResponse();
            }
            
            System.out.println("✅ Found " + enrollments.size() + " enrollment(s)");
            
            // Step 2: Extract unique course IDs
            Set<UUID> courseIds = new HashSet<>();
            for (Enrollment enrollment : enrollments) {
                if ("active".equalsIgnoreCase(enrollment.getStatus())) {
                    courseIds.add(enrollment.getCourseId());
                }
            }
            
            System.out.println("✅ Found " + courseIds.size() + " active course(s)");
            
            // Step 3: Get all courses
            Map<UUID, Course> coursesMap = new HashMap<>();
            List<Course> courses = courseRepository.findAllById(courseIds);
            for (Course course : courses) {
                coursesMap.put(course.getId(), course);
            }
            
            // Step 4: Get all schedules for these courses
            List<Schedule> allSchedules = new ArrayList<>();
            for (UUID courseId : courseIds) {
                List<Schedule> schedules = scheduleRepository.findByCourseId(courseId);
                allSchedules.addAll(schedules);
            }
            
            System.out.println("✅ Found " + allSchedules.size() + " schedule(s)");
            
            // Step 5: Pre-calculate events for each schedule
            List<Map<String, Object>> events = new ArrayList<>();
            
            for (Schedule schedule : allSchedules) {
                Course course = coursesMap.get(schedule.getCourseId());
                if (course == null) {
                    continue;
                }
                
                List<Map<String, Object>> scheduleEvents = calculateScheduleEvents(
                    schedule, 
                    startDate, 
                    endDate, 
                    maximumCount
                );
                events.addAll(scheduleEvents);
            }
            
            // Sort events by startTime
            events.sort((e1, e2) -> {
                String start1 = (String) e1.get("startTime");
                String start2 = (String) e2.get("startTime");
                return start1.compareTo(start2);
            });
            
            System.out.println("✅ Generated " + events.size() + " event(s)");
            
            // Step 6: Build response
            List<Map<String, Object>> coursesList = new ArrayList<>();
            for (Course course : courses) {
                Map<String, Object> courseData = new HashMap<>();
                courseData.put("courseId", course.getId().toString());
                courseData.put("name", course.getName());
                courseData.put("teacherName", course.getTeacherName());
                courseData.put("location", course.getLocation());
                courseData.put("description", course.getDescription());
                coursesList.add(courseData);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("courses", coursesList);
            response.put("events", events);
            
            System.out.println("========================================");
            return response;
            
        } catch (Exception e) {
            System.err.println("❌ Error getting class schedules: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get class schedules: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate schedule events based on recurrence rules
     */
    private List<Map<String, Object>> calculateScheduleEvents(Schedule schedule,
                                                                LocalDate startDate,
                                                                LocalDate endDate,
                                                                Integer maximumCount) {
        List<Map<String, Object>> events = new ArrayList<>();
        
        LocalDate scheduleStartDate = schedule.getStartDate();
        LocalTime startTime = schedule.getStartTime();
        Long durationMinutes = schedule.getDurationMinutes();
        String recurrenceRule = schedule.getRecurrenceRule();
        String dayOfWeekStr = schedule.getDayOfWeek();
        UUID courseId = schedule.getCourseId();
        
        // Use effective start date (max of schedule start and requested start)
        LocalDate effectiveStartDate = scheduleStartDate.isAfter(startDate) ? scheduleStartDate : startDate;
        
        // Parse recurrence rule or use dayOfWeek
        if (recurrenceRule != null && !recurrenceRule.isEmpty()) {
            // Try to parse RRULE format
            if (recurrenceRule.toUpperCase().startsWith("FREQ=")) {
                events.addAll(parseRRULE(recurrenceRule, scheduleStartDate, startTime, durationMinutes, 
                                       courseId, effectiveStartDate, endDate, maximumCount));
            } else {
                // Fall back to simple recurrence patterns
                events.addAll(parseSimpleRecurrence(recurrenceRule, scheduleStartDate, startTime, 
                                                   durationMinutes, courseId, effectiveStartDate, 
                                                   endDate, maximumCount, dayOfWeekStr));
            }
        } else {
            // Use dayOfWeek for weekly recurrence
            events.addAll(calculateWeeklyEvents(dayOfWeekStr, scheduleStartDate, startTime, 
                                              durationMinutes, courseId, effectiveStartDate, 
                                              endDate, maximumCount));
        }
        
        return events;
    }
    
    /**
     * Parse RRULE format (e.g., "FREQ=MONTHLY;BYMONTHDAY=5")
     */
    private List<Map<String, Object>> parseRRULE(String rrule,
                                                   LocalDate scheduleStartDate,
                                                   LocalTime startTime,
                                                   Long durationMinutes,
                                                   UUID courseId,
                                                   LocalDate startDate,
                                                   LocalDate endDate,
                                                   Integer maximumCount) {
        List<Map<String, Object>> events = new ArrayList<>();
        
        // Parse RRULE components
        String[] parts = rrule.toUpperCase().split(";");
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
            freq = "WEEKLY"; // Default
        }
        
        LocalDate currentDate = startDate;
        int count = 0;
        
        switch (freq) {
            case "DAILY":
                while (!currentDate.isAfter(endDate) && (maximumCount == null || count < maximumCount)) {
                    if (!currentDate.isBefore(scheduleStartDate)) {
                        events.add(createEvent(courseId, currentDate, startTime, durationMinutes));
                        count++;
                    }
                    currentDate = currentDate.plusDays(1);
                }
                break;
                
            case "WEEKLY":
                DayOfWeek targetDay = parseDayOfWeekString(byDay);
                if (targetDay == null) {
                    // Fall back to schedule's dayOfWeek
                    targetDay = parseDayOfWeek(scheduleStartDate.getDayOfWeek().toString());
                }
                
                // Find first occurrence
                while (currentDate.getDayOfWeek() != targetDay && !currentDate.isAfter(endDate)) {
                    currentDate = currentDate.plusDays(1);
                }
                
                while (!currentDate.isAfter(endDate) && (maximumCount == null || count < maximumCount)) {
                    if (!currentDate.isBefore(scheduleStartDate)) {
                        events.add(createEvent(courseId, currentDate, startTime, durationMinutes));
                        count++;
                    }
                    currentDate = currentDate.plusWeeks(1);
                }
                break;
                
            case "MONTHLY":
                if (byMonthDay != null) {
                    // Monthly on specific day (e.g., 5th of each month)
                    currentDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), 
                                              Math.min(byMonthDay, startDate.lengthOfMonth()));
                    
                    if (currentDate.isBefore(startDate)) {
                        currentDate = currentDate.plusMonths(1);
                        currentDate = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), 
                                                  Math.min(byMonthDay, currentDate.lengthOfMonth()));
                    }
                    
                    while (!currentDate.isAfter(endDate) && (maximumCount == null || count < maximumCount)) {
                        if (!currentDate.isBefore(scheduleStartDate)) {
                            events.add(createEvent(courseId, currentDate, startTime, durationMinutes));
                            count++;
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
                    // Monthly on same day of week (e.g., first Monday)
                    DayOfWeek dow = scheduleStartDate.getDayOfWeek();
                    int weekOfMonth = (scheduleStartDate.getDayOfMonth() - 1) / 7 + 1;
                    
                    currentDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), 1);
                    while (currentDate.getDayOfWeek() != dow) {
                        currentDate = currentDate.plusDays(1);
                    }
                    currentDate = currentDate.plusWeeks(weekOfMonth - 1);
                    
                    if (currentDate.isBefore(startDate)) {
                        currentDate = currentDate.plusMonths(1);
                        currentDate = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), 1);
                        while (currentDate.getDayOfWeek() != dow) {
                            currentDate = currentDate.plusDays(1);
                        }
                        currentDate = currentDate.plusWeeks(weekOfMonth - 1);
                    }
                    
                    while (!currentDate.isAfter(endDate) && (maximumCount == null || count < maximumCount)) {
                        if (!currentDate.isBefore(scheduleStartDate)) {
                            events.add(createEvent(courseId, currentDate, startTime, durationMinutes));
                            count++;
                        }
                        currentDate = currentDate.plusMonths(1);
                        LocalDate firstOfMonth = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), 1);
                        while (firstOfMonth.getDayOfWeek() != dow) {
                            firstOfMonth = firstOfMonth.plusDays(1);
                        }
                        currentDate = firstOfMonth.plusWeeks(weekOfMonth - 1);
                    }
                }
                break;
                
            default:
                // Default to weekly
                events.addAll(calculateWeeklyEvents(scheduleStartDate.getDayOfWeek().toString(), 
                                                   scheduleStartDate, startTime, durationMinutes, 
                                                   courseId, startDate, endDate, maximumCount));
        }
        
        return events;
    }
    
    /**
     * Parse simple recurrence patterns (e.g., "weekly", "monthly")
     */
    private List<Map<String, Object>> parseSimpleRecurrence(String recurrence,
                                                              LocalDate scheduleStartDate,
                                                              LocalTime startTime,
                                                              Long durationMinutes,
                                                              UUID courseId,
                                                              LocalDate startDate,
                                                              LocalDate endDate,
                                                              Integer maximumCount,
                                                              String dayOfWeekStr) {
        List<Map<String, Object>> events = new ArrayList<>();
        
        switch (recurrence.toLowerCase()) {
            case "weekly":
                events.addAll(calculateWeeklyEvents(dayOfWeekStr, scheduleStartDate, startTime, 
                                                  durationMinutes, courseId, startDate, endDate, maximumCount));
                break;
            case "monthly":
                // Monthly on the same day
                LocalDate currentDate = scheduleStartDate.isBefore(startDate) ? 
                                       LocalDate.of(startDate.getYear(), startDate.getMonth(), 
                                                   scheduleStartDate.getDayOfMonth()) : scheduleStartDate;
                if (currentDate.isBefore(startDate)) {
                    currentDate = currentDate.plusMonths(1);
                }
                int count = 0;
                while (!currentDate.isAfter(endDate) && (maximumCount == null || count < maximumCount)) {
                    events.add(createEvent(courseId, currentDate, startTime, durationMinutes));
                    count++;
                    currentDate = currentDate.plusMonths(1);
                    if (currentDate.lengthOfMonth() < scheduleStartDate.getDayOfMonth()) {
                        currentDate = LocalDate.of(currentDate.getYear(), currentDate.getMonth(), 
                                                  currentDate.lengthOfMonth());
                    }
                }
                break;
            case "daily":
                LocalDate dailyDate = scheduleStartDate.isBefore(startDate) ? startDate : scheduleStartDate;
                int dailyCount = 0;
                while (!dailyDate.isAfter(endDate) && (maximumCount == null || dailyCount < maximumCount)) {
                    events.add(createEvent(courseId, dailyDate, startTime, durationMinutes));
                    dailyCount++;
                    dailyDate = dailyDate.plusDays(1);
                }
                break;
            default:
                // Default to weekly
                events.addAll(calculateWeeklyEvents(dayOfWeekStr, scheduleStartDate, startTime, 
                                                  durationMinutes, courseId, startDate, endDate, maximumCount));
        }
        
        return events;
    }
    
    /**
     * Calculate weekly recurring events
     */
    private List<Map<String, Object>> calculateWeeklyEvents(String dayOfWeekStr,
                                                              LocalDate scheduleStartDate,
                                                              LocalTime startTime,
                                                              Long durationMinutes,
                                                              UUID courseId,
                                                              LocalDate startDate,
                                                              LocalDate endDate,
                                                              Integer maximumCount) {
        List<Map<String, Object>> events = new ArrayList<>();
        
        DayOfWeek targetDay = parseDayOfWeek(dayOfWeekStr);
        if (targetDay == null) {
            targetDay = scheduleStartDate.getDayOfWeek();
        }
        
        // Find first occurrence on or after startDate
        LocalDate currentDate = startDate;
        while (currentDate.getDayOfWeek() != targetDay && !currentDate.isAfter(endDate)) {
            currentDate = currentDate.plusDays(1);
        }
        
        // If we haven't reached scheduleStartDate, wait until then
        if (currentDate.isBefore(scheduleStartDate)) {
            currentDate = scheduleStartDate;
            while (currentDate.getDayOfWeek() != targetDay && !currentDate.isAfter(endDate)) {
                currentDate = currentDate.plusDays(1);
            }
        }
        
        int count = 0;
        while (!currentDate.isAfter(endDate) && (maximumCount == null || count < maximumCount)) {
            events.add(createEvent(courseId, currentDate, startTime, durationMinutes));
            count++;
            currentDate = currentDate.plusWeeks(1);
        }
        
        return events;
    }
    
    /**
     * Create an event map
     */
    private Map<String, Object> createEvent(UUID courseId, LocalDate date, LocalTime startTime, Long durationMinutes) {
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);
        
        Map<String, Object> event = new HashMap<>();
        event.put("courseId", courseId.toString());
        event.put("startTime", startDateTime.atOffset(ZoneOffset.UTC).toString());
        event.put("endTime", endDateTime.atOffset(ZoneOffset.UTC).toString());
        event.put("durationMinutes", durationMinutes.intValue());
        
        return event;
    }
    
    /**
     * Parse day of week string to DayOfWeek enum
     */
    private DayOfWeek parseDayOfWeek(String dayOfWeek) {
        if (dayOfWeek == null) {
            return null;
        }
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
                return null;
        }
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
     * Build empty response
     */
    private Map<String, Object> buildEmptyResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("courses", new ArrayList<>());
        response.put("events", new ArrayList<>());
        return response;
    }
}
