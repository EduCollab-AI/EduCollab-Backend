package com.educollab.controller;

import com.educollab.model.Student;
import com.educollab.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@CrossOrigin(origins = "*")
public class StudentController {
    
    @Autowired
    private StudentService studentService;
    
    @GetMapping
    public ResponseEntity<List<Student>> getStudentsByParentId(@RequestParam("associated_parent_id") String parentId) {
        System.out.println("Get students endpoint accessed for parentId: " + parentId);
        List<Student> students = studentService.getStudentsByParentId(parentId);
        return ResponseEntity.ok(students);
    }
}
