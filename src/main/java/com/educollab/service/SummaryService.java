package com.educollab.service;

import com.educollab.model.Course;
import com.educollab.model.Enrollment;
import com.educollab.model.PaymentEvent;
import com.educollab.model.Schedule;
import com.educollab.repository.CourseRepository;
import com.educollab.repository.EnrollmentRepository;
import com.educollab.repository.PaymentEventRepository;
import com.educollab.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SummaryService {
    
    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(1970, 1, 1);
    
    @Autowired
    private ClassScheduleService classScheduleService;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private PaymentEventRepository paymentEventRepository;
    
    @Transactional(readOnly = true)
    public Map<String, Object> getStudentSummary(String studentIdStr) {
        try {
            UUID studentId = UUID.fromString(studentIdStr);
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.minusDays(1);
            if (endDate.isBefore(DEFAULT_START_DATE)) {
                endDate = DEFAULT_START_DATE;
            }
            
            // Fetch enrollments to determine relevant courses
            List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
            if (enrollments.isEmpty()) {
                return buildEmptySummary(studentIdStr);
            }
            
            Set<UUID> courseIds = new HashSet<>();
            for (Enrollment enrollment : enrollments) {
                if (enrollment.getCourseId() != null) {
                    courseIds.add(enrollment.getCourseId());
                }
            }
            if (courseIds.isEmpty()) {
                return buildEmptySummary(studentIdStr);
            }
            
            List<Course> courses = courseRepository.findAllById(courseIds);
            Map<UUID, Course> courseMap = new HashMap<>();
            Map<String, UUID> courseNameLookup = new HashMap<>();
            for (Course course : courses) {
                courseMap.put(course.getId(), course);
                if (course.getName() != null) {
                    courseNameLookup.put(course.getName().toLowerCase(Locale.ROOT), course.getId());
                }
            }
            
            // Fetch schedules for planned calculation
            List<Schedule> schedules = scheduleRepository.findByCourseIdIn(courseIds);
            Map<UUID, List<Schedule>> schedulesByCourse = new HashMap<>();
            for (Schedule schedule : schedules) {
                schedulesByCourse
                    .computeIfAbsent(schedule.getCourseId(), key -> new ArrayList<>())
                    .add(schedule);
            }
            
            // Fetch paid payment events before today
            List<PaymentEvent> paidEvents = paymentEventRepository.findByStudentIdAndStatusAndDueDateBefore(
                studentId,
                "paid",
                today
            );
            Map<UUID, BigDecimal> totalPaidByCourse = aggregatePaymentsByCourse(paidEvents, courseNameLookup);
            
            // Fetch historical schedule events and aggregate minutes taken
            Map<String, Object> scheduleResponse = classScheduleService.getClassSchedules(
                studentIdStr,
                DEFAULT_START_DATE,
                endDate,
                null
            );
            Map<UUID, Long> minutesTakenByCourse = aggregateMinutesByCourse(scheduleResponse);
            
            // Build summary per course
            List<Map<String, Object>> courseSummaries = new ArrayList<>();
            for (Course course : courses) {
                UUID courseId = course.getId();
                
                long minutesTaken = minutesTakenByCourse.getOrDefault(courseId, 0L);
                long plannedMinutes = calculatePlannedMinutes(course, schedulesByCourse.getOrDefault(courseId, Collections.emptyList()));
                long pendingMinutes = Math.max(plannedMinutes - minutesTaken, 0L);
                
                Map<String, Object> courseSummary = new HashMap<>();
                courseSummary.put("courseId", courseId.toString());
                courseSummary.put("courseName", course.getName());
                courseSummary.put("totalPaidAmount", totalPaidByCourse.getOrDefault(courseId, BigDecimal.ZERO));
                courseSummary.put("hoursTaken", minutesToHours(minutesTaken));
                courseSummary.put("pendingHours", minutesToHours(pendingMinutes));
                
                courseSummaries.add(courseSummary);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("studentId", studentIdStr);
            response.put("summaryDate", today.toString());
            response.put("courses", courseSummaries);
            
            return response;
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid studentId format: " + studentIdStr);
        }
    }
    
    private Map<String, Object> buildEmptySummary(String studentId) {
        Map<String, Object> response = new HashMap<>();
        response.put("studentId", studentId);
        response.put("summaryDate", LocalDate.now().toString());
        response.put("courses", Collections.emptyList());
        return response;
    }
    
    private Map<UUID, BigDecimal> aggregatePaymentsByCourse(List<PaymentEvent> paidEvents, Map<String, UUID> courseNameLookup) {
        Map<UUID, BigDecimal> totals = new HashMap<>();
        for (PaymentEvent event : paidEvents) {
            if (event.getItem() == null) {
                continue;
            }
            UUID courseId = courseNameLookup.get(event.getItem().toLowerCase(Locale.ROOT));
            if (courseId == null) {
                continue;
            }
            totals.merge(courseId, safeAmount(event.getAmount()), BigDecimal::add);
        }
        return totals;
    }
    
    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
    
    private Map<UUID, Long> aggregateMinutesByCourse(Map<String, Object> scheduleResponse) {
        Map<UUID, Long> minutesByCourse = new HashMap<>();
        if (scheduleResponse == null) {
            return minutesByCourse;
        }
        Object eventsObj = scheduleResponse.get("events");
        if (!(eventsObj instanceof List)) {
            return minutesByCourse;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) eventsObj;
        for (Map<String, Object> event : events) {
            String courseIdStr = (String) event.get("courseId");
            Object durationObj = event.get("durationMinutes");
            String startTimeStr = (String) event.get("startTime");
            
            if (courseIdStr == null || durationObj == null || startTimeStr == null) {
                continue;
            }
            
            OffsetDateTime startTime;
            try {
                startTime = OffsetDateTime.parse(startTimeStr);
            } catch (Exception ex) {
                continue;
            }
            
            if (!startTime.toLocalDate().isBefore(LocalDate.now())) {
                continue;
            }
            
            UUID courseId;
            try {
                courseId = UUID.fromString(courseIdStr);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            
            long durationMinutes = ((Number) durationObj).longValue();
            minutesByCourse.merge(courseId, durationMinutes, Long::sum);
        }
        return minutesByCourse;
    }
    
    private long calculatePlannedMinutes(Course course, List<Schedule> schedules) {
        if (course == null || course.getTotalSessions() == null || course.getTotalSessions() <= 0 || schedules.isEmpty()) {
            return 0L;
        }
        
        int totalSessions = course.getTotalSessions();
        int scheduleCount = schedules.size();
        int baseSessionsPerSchedule = totalSessions / scheduleCount;
        int remainderSessions = totalSessions % scheduleCount;
        
        List<Schedule> sortedSchedules = new ArrayList<>(schedules);
        sortedSchedules.sort(Comparator.comparing(Schedule::getId));
        
        long plannedMinutes = 0L;
        for (int i = 0; i < sortedSchedules.size(); i++) {
            Schedule schedule = sortedSchedules.get(i);
            Long durationMinutes = schedule.getDurationMinutes();
            if (durationMinutes == null) {
                continue;
            }
            int sessionsForSchedule = baseSessionsPerSchedule + (i < remainderSessions ? 1 : 0);
            if (sessionsForSchedule <= 0) {
                continue;
            }
            plannedMinutes += durationMinutes * sessionsForSchedule;
        }
        
        return plannedMinutes;
    }
    
    private BigDecimal minutesToHours(long minutes) {
        if (minutes <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(minutes)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}


