package org.example.domain.repository;

import org.example.domain.model.Student;
import java.util.List;

public interface StudentRepository {
    void save(Student student);
    Student findById(String studentId);
    List<Student> findAll();
    void update(Student student);
    void delete(String studentId);
}