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
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
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
            
            // studentId is required when parent adds a course
            String studentIdStr = (String) request.get("studentId");
            if (studentIdStr == null || studentIdStr.isEmpty()) {
                throw new RuntimeException("studentId is required");
            }
            UUID studentId = UUID.fromString(studentIdStr);
            
            // Optional fields
            String description = request.get("description") != null ? (String) request.get("description") : null;
            Integer maxStudents = request.get("maxStudents") != null ? ((Number) request.get("maxStudents")).intValue() : null;
            
            System.out.println("Course Name: " + courseName);
            System.out.println("Teacher Name: " + teacherName);
            System.out.println("Location: " + location);
            System.out.println("Total Sessions: " + totalSessions);
            System.out.println("Course Start Date: " + courseStartDateStr);
            System.out.println("Student ID: " + studentIdStr);
            System.out.println("Description: " + description);
            
            // Parse course start date
            LocalDate courseStartDate = LocalDate.parse(courseStartDateStr);
            
            // Validate that student exists
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentIdStr));
            System.out.println("✅ Student validated: " + student.getName() + " (ID: " + studentId + ")");
            
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
            
            // Step 5: Create enrollment for student in this course
            // Check if enrollment already exists
            boolean enrollmentExists = enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId);
            
            if (!enrollmentExists) {
                Enrollment enrollment = new Enrollment(courseId, studentId);
                enrollment.setEnrolledAt(LocalDateTime.now());
                enrollment.setStatus("active");
                Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
                System.out.println("✅ Created enrollment for student: " + studentId + " in course: " + courseId);
                System.out.println("✅ Enrollment ID: " + savedEnrollment.getId());
            } else {
                System.out.println("ℹ️ Enrollment already exists for this student and course");
            }
            
            System.out.println("========================================");
            
            // Step 6: Build response
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
    
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteStudentEnrollment(String studentIdStr, String enrollmentIdStr) {
        try {
            System.out.println("========================================");
            System.out.println("🗑️ Deleting student enrollment:");
            System.out.println("Student ID: " + studentIdStr);
            System.out.println("Enrollment ID: " + enrollmentIdStr);
            System.out.println("========================================");
            
            UUID studentId = UUID.fromString(studentIdStr);
            UUID enrollmentId = UUID.fromString(enrollmentIdStr);
            
            // Step 1: Verify student exists
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentIdStr));
            
            System.out.println("✅ Found student: " + student.getName() + " (ID: " + studentId + ")");
            
            // Step 2: Verify enrollment exists and belongs to this student
            Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found with ID: " + enrollmentIdStr));
            
            if (!enrollment.getStudentId().equals(studentId)) {
                throw new RuntimeException("Enrollment does not belong to the specified student");
            }
            
            System.out.println("✅ Found enrollment: " + enrollmentId + " for course: " + enrollment.getCourseId());
            
            // Step 3: Get courseId from enrollment
            UUID courseId = enrollment.getCourseId();
            
            // Step 4: Delete all schedules for this course
            List<Schedule> schedules = scheduleRepository.findByCourseId(courseId);
            int schedulesDeletedCount = 0;
            if (!schedules.isEmpty()) {
                scheduleRepository.deleteAll(schedules);
                schedulesDeletedCount = schedules.size();
                System.out.println("✅ Deleted " + schedulesDeletedCount + " schedule(s) for course");
            } else {
                System.out.println("ℹ️ No schedules found for course");
            }
            
            // Step 5: Delete the enrollment
            enrollmentRepository.delete(enrollment);
            System.out.println("✅ Enrollment deleted successfully");
            System.out.println("========================================");
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Student enrollment and schedules deleted successfully");
            
            Map<String, Object> data = new HashMap<>();
            data.put("enrollmentId", enrollmentId.toString());
            data.put("courseId", courseId.toString());
            data.put("schedulesDeleted", schedulesDeletedCount);
            
            response.put("data", data);
            
            return response;
            
        } catch (Exception e) {
            System.err.println("❌ Error deleting student enrollment: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete student enrollment: " + e.getMessage());
            
            return errorResponse;
        }
    }
}

