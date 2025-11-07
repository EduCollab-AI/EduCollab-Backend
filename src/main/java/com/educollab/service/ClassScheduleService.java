package com.educollab.service;

import com.educollab.model.Course;
import com.educollab.model.Enrollment;
import com.educollab.model.Schedule;
import com.educollab.model.ScheduleException;
import com.educollab.model.Student;
import com.educollab.repository.CourseRepository;
import com.educollab.repository.EnrollmentRepository;
import com.educollab.repository.ScheduleExceptionRepository;
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
    private ScheduleExceptionRepository scheduleExceptionRepository;
    
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
            
            // Step 2: Extract unique course IDs and track status/inactive dates
            Set<UUID> courseIds = new HashSet<>();
            Map<UUID, String> courseStatusMap = new HashMap<>();
            Map<UUID, LocalDate> courseInactiveDateMap = new HashMap<>();
            
            for (Enrollment enrollment : enrollments) {
                UUID courseId = enrollment.getCourseId();
                courseIds.add(courseId);
                String status = enrollment.getStatus() != null ? enrollment.getStatus() : "active";
                
                if ("active".equalsIgnoreCase(status)) {
                    courseStatusMap.put(courseId, "active");
                    courseInactiveDateMap.remove(courseId);
                } else {
                    courseStatusMap.putIfAbsent(courseId, status);
                    LocalDate deactivatedDate = enrollment.getDeactivatedAt() != null
                        ? enrollment.getDeactivatedAt().toLocalDate()
                        : LocalDate.now();
                    LocalDate currentInactive = courseInactiveDateMap.get(courseId);
                    if (currentInactive == null || deactivatedDate.isBefore(currentInactive)) {
                        courseInactiveDateMap.put(courseId, deactivatedDate);
                    }
                }
            }
            
            System.out.println("✅ Found " + courseIds.size() + " course(s) (active + inactive)");
            
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
            
            // Fetch all schedule exceptions relevant to these schedules
            Set<UUID> scheduleIds = new HashSet<>();
            for (Schedule schedule : allSchedules) {
                scheduleIds.add(schedule.getId());
            }
            Map<UUID, List<ScheduleException>> exceptionsBySchedule = new HashMap<>();
            if (!scheduleIds.isEmpty()) {
                List<ScheduleException> exceptions = scheduleExceptionRepository.findByScheduleIdIn(scheduleIds);
                for (ScheduleException exception : exceptions) {
                    exceptionsBySchedule
                        .computeIfAbsent(exception.getScheduleId(), key -> new ArrayList<>())
                        .add(exception);
                }
            }
            
            // Count schedules per course (to divide totalSessions among schedules)
            Map<UUID, Integer> schedulesPerCourse = new HashMap<>();
            for (Schedule schedule : allSchedules) {
                UUID courseId = schedule.getCourseId();
                schedulesPerCourse.put(courseId, schedulesPerCourse.getOrDefault(courseId, 0) + 1);
            }
            
            System.out.println("📊 Schedules per course: " + schedulesPerCourse);
            
            // Step 5: Pre-calculate events for each schedule
            List<Map<String, Object>> events = new ArrayList<>();
            
            for (Schedule schedule : allSchedules) {
                Course course = coursesMap.get(schedule.getCourseId());
                if (course == null) {
                    continue;
                }
                
                int numberOfSchedulesForCourse = schedulesPerCourse.getOrDefault(schedule.getCourseId(), 1);
                LocalDate inactiveDate = courseInactiveDateMap.get(schedule.getCourseId());
                Map<String, ScheduleException> exceptionMap = new HashMap<>();
                for (ScheduleException exception : exceptionsBySchedule.getOrDefault(schedule.getId(), Collections.emptyList())) {
                    String key = buildExceptionKey(exception.getOriginalDate(), exception.getOriginalStartTime());
                    exceptionMap.put(key, exception);
                }
                
                List<Map<String, Object>> scheduleEvents = calculateScheduleEvents(
                    schedule, 
                    course,
                    numberOfSchedulesForCourse,
                    startDate, 
                    endDate, 
                    maximumCount,
                    inactiveDate,
                    exceptionMap
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
                String status = courseStatusMap.getOrDefault(course.getId(), "active");
                courseData.put("status", status);
                LocalDate inactiveDate = courseInactiveDateMap.get(course.getId());
                if (inactiveDate != null) {
                    courseData.put("inactiveDate", inactiveDate.toString());
                }
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
                                                                Course course,
                                                                int numberOfSchedulesForCourse,
                                                                LocalDate startDate,
                                                                LocalDate endDate,
                                                                Integer maximumCount,
                                                                LocalDate inactiveDate,
                                                                Map<String, ScheduleException> exceptionMap) {
        List<Map<String, Object>> events = new ArrayList<>();
        
        LocalDate scheduleStartDate = schedule.getStartDate();
        LocalTime startTime = schedule.getStartTime();
        Long durationMinutes = schedule.getDurationMinutes();
        String recurrenceRule = schedule.getRecurrenceRule();
        String dayOfWeekStr = schedule.getDayOfWeek();
        UUID courseId = schedule.getCourseId();
        
        // Use effective start date (max of schedule start and requested start)
        LocalDate effectiveStartDate = scheduleStartDate.isAfter(startDate) ? scheduleStartDate : startDate;
        
        // If enrollment is inactive before the effective start date, skip entirely
        if (inactiveDate != null && inactiveDate.isBefore(effectiveStartDate)) {
            System.out.println("ℹ️ Course is inactive before requested date range; skipping schedule events");
            return events;
        }
        
        // Calculate sessions per schedule: divide totalSessions by number of schedules for this course
        // Use integer division - if there's a remainder, it's acceptable (each schedule gets floor division)
        Integer totalSessions = course.getTotalSessions();
        int sessionsPerSchedule = totalSessions / numberOfSchedulesForCourse;
        
        // Calculate remaining sessions for THIS schedule based on sessionsPerSchedule
        int sessionsAlreadyOccurred = countSessionsOccurred(schedule, scheduleStartDate, effectiveStartDate);
        int countOfCoursesLeft = Math.max(0, sessionsPerSchedule - sessionsAlreadyOccurred);
        
        // Calculate effective maximum count: min(maximumCount, remainingSessions)
        Integer effectiveMaxCount = maximumCount;
        if (effectiveMaxCount != null) {
            effectiveMaxCount = Math.min(maximumCount, countOfCoursesLeft);
        } else {
            effectiveMaxCount = countOfCoursesLeft;
        }
        
        System.out.println("📊 Course: " + course.getName() + ", Total Sessions: " + totalSessions + 
                          ", Number of Schedules: " + numberOfSchedulesForCourse +
                          ", Sessions per Schedule: " + sessionsPerSchedule +
                          ", Already Occurred (this schedule): " + sessionsAlreadyOccurred + 
                          ", Remaining (this schedule): " + countOfCoursesLeft + 
                          ", Effective Max Count: " + effectiveMaxCount +
                          (inactiveDate != null ? ", Inactive Date: " + inactiveDate : ""));
        
        // Parse recurrence rule or use dayOfWeek
        if (recurrenceRule != null && !recurrenceRule.isEmpty()) {
            // Try to parse RRULE format
            if (recurrenceRule.toUpperCase().startsWith("FREQ=")) {
                events.addAll(parseRRULE(recurrenceRule, scheduleStartDate, startTime, durationMinutes, 
                                       courseId, effectiveStartDate, endDate, effectiveMaxCount, inactiveDate, exceptionMap, schedule.getId()));
            } else {
                // Fall back to simple recurrence patterns
                events.addAll(parseSimpleRecurrence(recurrenceRule, scheduleStartDate, startTime, 
                                                   durationMinutes, courseId, effectiveStartDate, 
                                                   endDate, effectiveMaxCount, dayOfWeekStr, inactiveDate, exceptionMap, schedule.getId()));
            }
        } else {
            // Use dayOfWeek for weekly recurrence
            events.addAll(calculateWeeklyEvents(dayOfWeekStr, scheduleStartDate, startTime, 
                                              durationMinutes, courseId, schedule.getId(), effectiveStartDate, 
                                              endDate, effectiveMaxCount, inactiveDate, exceptionMap));
        }
        
        return events;
    }
    
    /**
     * Count how many sessions have already occurred from scheduleStartDate to currentDate
     */
    private int countSessionsOccurred(Schedule schedule, LocalDate scheduleStartDate, LocalDate currentDate) {
        if (currentDate.isBefore(scheduleStartDate) || currentDate.equals(scheduleStartDate)) {
            return 0; // No sessions have occurred yet
        }
        
        String recurrenceRule = schedule.getRecurrenceRule();
        String dayOfWeekStr = schedule.getDayOfWeek();
        
        // Count based on recurrence pattern
        if (recurrenceRule != null && !recurrenceRule.isEmpty()) {
            if (recurrenceRule.toUpperCase().startsWith("FREQ=")) {
                return countSessionsOccurredRRULE(recurrenceRule, scheduleStartDate, currentDate, dayOfWeekStr);
            } else {
                return countSessionsOccurredSimple(recurrenceRule, scheduleStartDate, currentDate, dayOfWeekStr);
            }
        } else {
            // Default to weekly
            return countSessionsOccurredWeekly(dayOfWeekStr, scheduleStartDate, currentDate);
        }
    }
    
    /**
     * Count sessions for RRULE format
     */
    private int countSessionsOccurredRRULE(String rrule, LocalDate scheduleStartDate, LocalDate currentDate, String dayOfWeekStr) {
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
            freq = "WEEKLY";
        }
        
        switch (freq) {
            case "DAILY":
                return (int) java.time.temporal.ChronoUnit.DAYS.between(scheduleStartDate, currentDate);
            case "WEEKLY":
                DayOfWeek targetDay = parseDayOfWeekString(byDay);
                if (targetDay == null) {
                    targetDay = parseDayOfWeek(dayOfWeekStr);
                    if (targetDay == null) {
                        targetDay = scheduleStartDate.getDayOfWeek();
                    }
                }
                // Count sessions that occurred strictly before currentDate
                int weeks = 0;
                LocalDate weeklyDate = scheduleStartDate;
                while (weeklyDate.isBefore(currentDate)) {
                    if (weeklyDate.getDayOfWeek() == targetDay) {
                        weeks++;
                    }
                    weeklyDate = weeklyDate.plusDays(1);
                }
                return weeks;
            case "MONTHLY":
                if (byMonthDay != null) {
                    // Count sessions that occurred strictly before currentDate
                    int months = 0;
                    LocalDate monthlyDate = scheduleStartDate;
                    while (monthlyDate.isBefore(currentDate)) {
                        months++;
                        monthlyDate = monthlyDate.plusMonths(1);
                        // Adjust if month doesn't have enough days
                        if (monthlyDate.lengthOfMonth() < byMonthDay) {
                            monthlyDate = LocalDate.of(monthlyDate.getYear(), monthlyDate.getMonth(), 
                                                      monthlyDate.lengthOfMonth());
                        } else {
                            monthlyDate = LocalDate.of(monthlyDate.getYear(), monthlyDate.getMonth(), byMonthDay);
                        }
                    }
                    return months;
                } else {
                    // Monthly on same day of week - count sessions that occurred strictly before currentDate
                    int months = 0;
                    LocalDate monthlyDate2 = scheduleStartDate;
                    while (monthlyDate2.isBefore(currentDate)) {
                        months++;
                        monthlyDate2 = monthlyDate2.plusMonths(1);
                    }
                    return months;
                }
            default:
                return countSessionsOccurredWeekly(dayOfWeekStr, scheduleStartDate, currentDate);
        }
    }
    
    /**
     * Count sessions for simple recurrence patterns
     */
    private int countSessionsOccurredSimple(String recurrence, LocalDate scheduleStartDate, LocalDate currentDate, String dayOfWeekStr) {
        switch (recurrence.toLowerCase()) {
            case "daily":
                return (int) java.time.temporal.ChronoUnit.DAYS.between(scheduleStartDate, currentDate);
            case "weekly":
                return countSessionsOccurredWeekly(dayOfWeekStr, scheduleStartDate, currentDate);
            case "monthly":
                return (int) java.time.temporal.ChronoUnit.MONTHS.between(scheduleStartDate, currentDate);
            default:
                return countSessionsOccurredWeekly(dayOfWeekStr, scheduleStartDate, currentDate);
        }
    }
    
    /**
     * Count sessions for weekly recurrence
     */
    private int countSessionsOccurredWeekly(String dayOfWeekStr, LocalDate scheduleStartDate, LocalDate currentDate) {
        DayOfWeek targetDay = parseDayOfWeek(dayOfWeekStr);
        if (targetDay == null) {
            targetDay = scheduleStartDate.getDayOfWeek();
        }
        
        int count = 0;
        LocalDate date = scheduleStartDate;
        while (!date.isAfter(currentDate)) {
            if (date.getDayOfWeek() == targetDay && date.isBefore(currentDate)) {
                count++;
            }
            date = date.plusDays(1);
            if (date.isAfter(currentDate)) break;
        }
        return count;
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
                                                   Integer maximumCount,
                                                   LocalDate inactiveDate,
                                                   Map<String, ScheduleException> exceptionMap,
                                                   UUID scheduleId) {
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
                    if (inactiveDate != null && currentDate.isAfter(inactiveDate)) {
                        break;
                    }
                    Map<String, Object> overrideEvent = applyExceptionIfPresent(courseId, scheduleId, currentDate, startTime, durationMinutes, exceptionMap);
                    if (overrideEvent == null) {
                        events.add(createEvent(scheduleId, courseId, currentDate, startTime, durationMinutes));
                    } else if (!overrideEvent.isEmpty()) {
                        events.add(overrideEvent);
                    }
                    count++;
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
                    if (inactiveDate != null && currentDate.isAfter(inactiveDate)) {
                        break;
                    }
                    Map<String, Object> overrideEvent = applyExceptionIfPresent(courseId, scheduleId, currentDate, startTime, durationMinutes, exceptionMap);
                    if (overrideEvent == null) {
                        events.add(createEvent(scheduleId, courseId, currentDate, startTime, durationMinutes));
                    } else if (!overrideEvent.isEmpty()) {
                        events.add(overrideEvent);
                    }
                    count++;
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
                        if (inactiveDate != null && currentDate.isAfter(inactiveDate)) {
                            break;
                        }
                        Map<String, Object> overrideEvent = applyExceptionIfPresent(courseId, scheduleId, currentDate, startTime, durationMinutes, exceptionMap);
                        if (overrideEvent == null) {
                            events.add(createEvent(scheduleId, courseId, currentDate, startTime, durationMinutes));
                        } else if (!overrideEvent.isEmpty()) {
                            events.add(overrideEvent);
                        }
                        count++;
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
                        if (inactiveDate != null && currentDate.isAfter(inactiveDate)) {
                            break;
                        }
                        Map<String, Object> overrideEvent = applyExceptionIfPresent(courseId, scheduleId, currentDate, startTime, durationMinutes, exceptionMap);
                        if (overrideEvent == null) {
                            events.add(createEvent(scheduleId, courseId, currentDate, startTime, durationMinutes));
                        } else if (!overrideEvent.isEmpty()) {
                            events.add(overrideEvent);
                        }
                        count++;
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
                                                   courseId, scheduleId, startDate, endDate, maximumCount, inactiveDate, exceptionMap));
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
                                                              String dayOfWeekStr,
                                                              LocalDate inactiveDate,
                                                              Map<String, ScheduleException> exceptionMap,
                                                              UUID scheduleId) {
        List<Map<String, Object>> events = new ArrayList<>();
        
        switch (recurrence.toLowerCase()) {
            case "weekly":
                events.addAll(calculateWeeklyEvents(dayOfWeekStr, scheduleStartDate, startTime, 
                                                  durationMinutes, courseId, scheduleId, startDate, endDate, maximumCount, inactiveDate, exceptionMap));
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
                    if (inactiveDate != null && currentDate.isAfter(inactiveDate)) {
                        break;
                    }
                    Map<String, Object> overrideEvent = applyExceptionIfPresent(courseId, scheduleId, currentDate, startTime, durationMinutes, exceptionMap);
                    if (overrideEvent == null) {
                        events.add(createEvent(scheduleId, courseId, currentDate, startTime, durationMinutes));
                    } else if (!overrideEvent.isEmpty()) {
                        events.add(overrideEvent);
                    }
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
                    if (inactiveDate != null && dailyDate.isAfter(inactiveDate)) {
                        break;
                    }
                    Map<String, Object> overrideEvent = applyExceptionIfPresent(courseId, scheduleId, dailyDate, startTime, durationMinutes, exceptionMap);
                    if (overrideEvent == null) {
                        events.add(createEvent(scheduleId, courseId, dailyDate, startTime, durationMinutes));
                    } else if (!overrideEvent.isEmpty()) {
                        events.add(overrideEvent);
                    }
                    dailyCount++;
                    dailyDate = dailyDate.plusDays(1);
                }
                break;
            default:
                // Default to weekly
                events.addAll(calculateWeeklyEvents(dayOfWeekStr, scheduleStartDate, startTime, 
                                                  durationMinutes, courseId, scheduleId, startDate, endDate, maximumCount, inactiveDate, exceptionMap));
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
                                                              UUID scheduleId,
                                                              LocalDate startDate,
                                                              LocalDate endDate,
                                                              Integer maximumCount,
                                                              LocalDate inactiveDate,
                                                              Map<String, ScheduleException> exceptionMap) {
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
            if (inactiveDate != null && currentDate.isAfter(inactiveDate)) {
                break;
            }
            Map<String, Object> overrideEvent = applyExceptionIfPresent(courseId, scheduleId, currentDate, startTime, durationMinutes, exceptionMap);
            if (overrideEvent == null) {
                events.add(createEvent(scheduleId, courseId, currentDate, startTime, durationMinutes));
            } else if (!overrideEvent.isEmpty()) {
                events.add(overrideEvent);
            }
            count++;
            currentDate = currentDate.plusWeeks(1);
        }
        
        return events;
    }
    
    /**
     * Create an event map
     */
    private Map<String, Object> createEvent(UUID scheduleId, UUID courseId, LocalDate date, LocalTime startTime, Long durationMinutes) {
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);
        
        Map<String, Object> event = new HashMap<>();
        event.put("scheduleId", scheduleId.toString());
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

    private Map<String, Object> applyExceptionIfPresent(UUID courseId,
                                                         UUID scheduleId,
                                                         LocalDate originalDate,
                                                         LocalTime originalStartTime,
                                                         Long durationMinutes,
                                                         Map<String, ScheduleException> exceptionMap) {
        String key = buildExceptionKey(originalDate, originalStartTime);
        ScheduleException exception = exceptionMap.get(key);
        if (exception == null) {
            return null;
        }
        
        if (Boolean.TRUE.equals(exception.getIsCancelled())) {
            return Collections.emptyMap();
        }
        
        LocalDate eventDate = exception.getNewDate() != null ? exception.getNewDate() : originalDate;
        LocalTime eventStart = exception.getNewStartTime() != null ? exception.getNewStartTime() : originalStartTime;
        Long eventDuration = exception.getNewDurationMinutes() != null ? exception.getNewDurationMinutes() : durationMinutes;
        
        return createEvent(scheduleId, courseId, eventDate, eventStart, eventDuration);
    }
    
    private String buildExceptionKey(LocalDate date, LocalTime time) {
        return date.toString() + "|" + time.toString();
    }
}
