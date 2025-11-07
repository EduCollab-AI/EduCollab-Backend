package com.educollab.service;

import com.educollab.model.Student;
import com.educollab.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {
    
    @Autowired
    private StudentRepository studentRepository;
    
    public List<Student> getStudentsByParentId(String parentId) {
        System.out.println("Fetching students for parentId: " + parentId);
        return studentRepository.findByAssociatedParentId(parentId);
    }
}
